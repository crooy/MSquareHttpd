package MSquareHttpd;
/*
 M2HTTPD: A tiny yet pipelined, non-blocking, extensible HTTPD.

 Author: Matthew Might
 Site:   http://matt.might.net/

 M2HTTPD is implemented using a pipeline of transducers.

 A transducer is a kind of coroutine (a communicating process) that
 consumes upstream inputs and sends outputs downstream.

 M2HTTPD has four transducers chained together into a pipeline:

 (1) Phase 1 listens for incoming connections and passes
     connected sockets downstream.

 (2) Phase 2 wraps a socket in a connection object and parses
     an HTTP request out of it.

 (3) Phase 3 routes a request to the appropriate request handler,
     and if the handler produces a reply, the reply is sent 
     downstream.

 (4) Phase 4 consumes replies and pushes them onto the network.
 
 By slicing a task into phases, it is possible to operate on multiple
 tasks at the same time, thereby increasing the throughput of the
 overall system.

 No blocking IO calls are used anywhere, which makes some kinds of
 attacks against the HTTPD (e.g. SYN floods) ineffective.

 */

import java.net.InetAddress

import com.weiglewilczek.slf4s.Logging

import MSquareHttpd._


/**
 * An extensible, piplined HTTPD that uses non-blocking IO.
 */
class M2HTTPD {

  /**
   * The port on which the HTTPD listens.
   *
   * By default, 1701.
   */
  var port = 1701;

  /**
   * The address to which the HTTPD listens.
   *
   * By default, null. (null means list to all local addresses.)
   */
  var localAddress: InetAddress = null;

  /**
   * A coroutine that listens to a port to produce sockets.
   */
  val listen = new PortListener(localAddress, port);

  /**
   * A coroutine that turns sockets into HTTP requests.
   */
  val connect =  new ConnectionManager(this);

  /**
   * A coroutine that routes and processes requests.
   */
  val process = new RequestProcessor;

  /**
   * A coroutine that sends replies
   */
  val reply = new ReplySender;

  /**
   * A pipelined coroutine that ties together all of the stages of the HTTPD.
   */
  val system = listen ==> connect ==> process ==> reply

  /**
   * Starts up the HTTPD.
   */
  def startup() {
    system.startup();
  }
}

/*  Demo servers  */

/**
 * A driver that starts the M2HTTPD with the default (all-404) request handler.
 */
object EmptyHTTPD extends Logging {
  def main(args: Array[String]) {
    logger.debug("Default M2HTTPD running...")

    val httpd = new M2HTTPD;
    httpd.startup()

  }
}

/**
 * A driver that starts the M2HTTPD with the simple file server request handler.
 */
object SimpleHTTPD extends Logging {
  def main(args: Array[String]) {
    logger.debug("Simple M2HTTPD running...")

    val httpd = new M2HTTPD;

    // By default, serve from ./www/ in the current directory.
    var docRoot = "/Users/ronald/Documents/dev/websites/ScrumSquare/build/site/";

    args match {
      case Array(newRoot) => docRoot = newRoot
      case _ => ()
    }

    logger.debug("Serving from " + docRoot);

    // If you want to take control of the HTTPD, then take control of
    // the hostRouter, and create a request handler:
    httpd.process.hostRouter =
      new DefaultHostRouter(new SimpleFileRequestHandler(docRoot));

    httpd.startup()

  }
}

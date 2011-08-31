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
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.channels.FileChannel
import java.nio.channels.Selector
import java.nio.channels.spi.SelectorProvider
import java.nio.channels.SelectionKey
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import com.weiglewilczek.slf4s.Logging

/**
 A coroutine is a process (in this case a thread) that communicates with other coroutines.
 */
trait Coroutine extends Runnable {
  def start () {
    val myThread = new Thread(this) ;
    myThread.start() ;
  }
}


/**
 A O-producer is a coroutine that produces type-O objects for consumption by other coroutines.
 */
trait Producer[O] extends Coroutine {
  private val outputs =
    new java.util.concurrent.ArrayBlockingQueue[O] (1024) ;

  /**
   Called by the coroutine's <code>run</code> method when it has produced a new output.
   */
  protected def put (output : O) {
    outputs.put(output) ;
  }

  /**
   Called by an upstream consumer when it wants a new value from this coroutine.
   */
  def next () : O = {
    outputs.take() ;
  }

  /**
   Composes this producing coroutine with a transducing coroutine.

   @return A fused producing coroutine.
   */
  def ==>[O2] (t : Transducer[O,O2]) : Producer[O2] = {
    val that = this ;
    new Producer[O2] {
      def run () {
        while (true) {
          val o = that.next() ;
          t.accept(o);
          put(t.next()) ;
        }
      }
      
      override def start () {
        that.start() ;
        t.start() ;
        super.start() ;
      }
    }
  }

  /**
   Composes this producing coroutine with a consuming coroutine.

   @return A fused coroutine.
   */
  def ==> (consumer : Consumer[O]) : Coroutine = {
    val that = this ;
    new Coroutine {
      def run {
        while (true) {
          val o = that.next() ;
          consumer.accept(o) ;
        }
      }

      override def start {
        that.start() ;
        consumer.start() ;
        super.start() ;
      }
    }
  }
}


/**
 An I-consumer is a coroutine that consumes type-I objects.
 */
trait Consumer[I] extends Coroutine {
  private val inputs =
    new java.util.concurrent.ArrayBlockingQueue[I] (1024) ;

  /**
   Called when an external I-producer has a new input to provide.
   */
  def accept (input : I) {
    inputs.put(input) ;
  }

  /**
   Called by the coroutine itself when it needs the next input.
   */
  protected def get () : I = {
    inputs.take() ;
  }
}


/**
 An I,O-transducer consumes type-I objects and produces type-O objects.
 */
trait Transducer[I,O] extends Consumer[I] with Producer[O] 


/**
 Represents an HTTP request method.
 */
trait HTTPMethod

case object POST extends HTTPMethod
case object GET extends HTTPMethod









/**
 Converts HTTP status codes into their corresponding string.

 For example, 200 becomes "OK" and 404 becomes "Not Found".
 */
object HTTPCodeName {

  private var table = new Array[String](1000)

  table(200) = "OK"  
  table(400) = "Bad Request"  
  table(404) = "Not Found"

  def apply (code : Int) : String = table(code)
}



/**
 An extensible, piplined HTTPD that uses non-blocking IO.
 */
class M2HTTPD {

  /**
   The port on which the HTTPD listens.

   By default, 1701.
   */
  var port = 1701 ;

  /**
   The address to which the HTTPD listens.

   By default, null. (null means list to all local addresses.)
   */
  var localAddress : InetAddress = null ;


  /**
   A coroutine that listens to a port to produce sockets.
   */
  val listen = new PortListener (localAddress,port) ;
  
  /**
   A coroutine that turns sockets into HTTP requests.
   */
  val connect = new ConnectionManager(this) ;
  
  /**
   A coroutine that routes and processes requests.
   */
  val process = new RequestProcessor ;
  
  /**
   A coroutine that sends replies
   */
  val reply = new ReplySender ;


  /**
   A pipelined coroutine that ties together all of the stages of the HTTPD.
   */
  val system = listen ==> connect ==> process ==> reply

  /**
   Starts up the HTTPD.
   */
  def start () {
    system.start()
  }
}



/*  Demo servers  */



/**
 A driver that starts the M2HTTPD with the default (all-404) request handler.
 */
object EmptyHTTPD extends Logging {
  def main (args : Array[String]) {
    logger.debug("Default M2HTTPD running...") 
    
    val httpd = new M2HTTPD ;
    httpd.start()

  }
}

 

/**
 A driver that starts the M2HTTPD with the simple file server request handler.
 */
object SimpleHTTPD extends Logging {
  def main (args : Array[String]) {
    logger.debug("Simple M2HTTPD running...") 
    
    val httpd = new M2HTTPD ;

    // By default, serve from ./www/ in the current directory.
    var docRoot = "./www/" ;

    args match {
      case Array(newRoot) => docRoot = newRoot
      case _ => ()
    }

    logger.debug("Serving from " + docRoot) ;

    // If you want to take control of the HTTPD, then take control of
    // the hostRouter, and create a request handler:
    httpd.process.hostRouter = 
      new DefaultHostRouter(new SimpleFileRequestHandler(docRoot)) ; 

    httpd.start()

  }
}
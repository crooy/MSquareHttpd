package MSquareHttpd.Actors;
/**
 A coroutine that consumes client connections and produces HTTP requests from them.
 */
import MSquareHttpd._
import java.nio.channels.spi.SelectorProvider
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.io.IOException
import java.nio.channels.Selector
import scala.actors.threadpool.LinkedBlockingQueue
import com.weiglewilczek.slf4s.Logging
import scala.collection.mutable.LinkedList
import scala.collection.mutable.SynchronizedQueue

class ConnectionManager (val httpd : M2HTTPD) extends Transducer[SocketChannel,Request] with Logging {

  private val farm = new ThreadFarm(1024,10)

  override def startup () {
    farm.start();
    self.start();
    startSenders();
  }

  private object ConnectionSelector extends Runnable {

    private val newConnections = 
      new SynchronizedQueue[ClientConnection] () ;


    private val selector : Selector = 
      SelectorProvider.provider().openSelector() ;


    def add(conn : ClientConnection) {
      newConnections :+ conn
      this.selector.wakeup() ;
    }

    def start () {
      val thread = new Thread(this) ;
      thread.start () ;
    }

    def run () {

      logger.debug("Connection Selector initialized...") 
      while (true) {
        selector.select() ;


        val selectedKeys = selector.selectedKeys.iterator() ;
        logger.debug(" ** Reading keys selected.") ;

        while (selectedKeys.hasNext()) {
          val key = selectedKeys.next().asInstanceOf[SelectionKey] ;

          selectedKeys.remove() ;

          if (key.isValid()) {
            
            val conn = key.attachment().asInstanceOf[ClientConnection] ;

            // Remove the key so we don't immediately loop around to
            // race on the same connection.
            key.cancel() ;

            farm run {
              try {
                // Read whatever is available.
                conn.readMoreInput() ;

                // Return this connection to the read selector.
                add(conn) ;
              } catch {
                case (ioe : IOException) => {
                  // Socket shut down.
                  key.cancel() ;
                }
              }

              // Was enough input read to complete a request?
              if (conn.hasRequest) {
                send(conn.takeRequest());
              }
            }
          }
        }

        for (conn <- newConnections if (conn.isOpen)) 
            conn.socketChannel.register(this.selector, SelectionKey.OP_READ, conn) ;
        
      }
    }
  }

  def receive = {
    case AnyMessage(wrappedSocket:SocketChannel) =>
      val conn = new ClientConnection(httpd,wrappedSocket)
      logger.debug(" * Got a requesting connection.") ;
      ConnectionSelector.add(conn) ;
    case _ =>
      throw new RuntimeException("Unknown message")
  }
}



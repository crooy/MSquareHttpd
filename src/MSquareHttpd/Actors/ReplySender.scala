package MSquareHttpd.Actors;
/* 
 * A coroutine that consumes replies and sends them off without blocking.
 */

import java.nio.channels.spi.SelectorProvider
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.io.IOException
import scala.collection.mutable.SynchronizedQueue
import com.weiglewilczek.slf4s.Logging
import MSquareHttpd._


class ReplySender extends Consumer[Reply] with Logging{

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

      logger.debug("Reply Sender Selector initialized...") 
      while (true) {
        selector.select() ;


        val selectedKeys = selector.selectedKeys.iterator() ;
        logger.debug(" ** Writing keys selected.") ;

        while (selectedKeys.hasNext()) {
          val key = selectedKeys.next().asInstanceOf[SelectionKey] ;

          selectedKeys.remove() ;

          if (key.isValid()) {
            
            val conn = key.attachment().asInstanceOf[ClientConnection] ;
            
            key.cancel() ;

            if (!conn.isOpen) {
              conn.close() ;
            }
            
            farm run {
              try {
                logger.debug(" ** Sending reply!") ;
                val finished = conn.flushWriteBuffers() ;
                if (!finished) {
                  add(conn) ;
                } else {
                  conn.close() ;
                }
              } catch {
                case (ioe : IOException) => {
                  // Socket shut down.
                  key.cancel() ;
                }
              }
            }
          }
        }

        
        for (conn <- newConnections) 
          conn.socketChannel.register(this.selector, SelectionKey.OP_WRITE, conn) ;
        
      }
    }
  }
  
  def receive = {
    case AnyMessage(wrappedReply:Reply) =>
      val conn = wrappedReply.req.connection;
      conn.send(wrappedReply);
      logger.debug (" * Sending a reply.") ;
      ConnectionSelector.add(conn);
    case _ => throw new RuntimeException("unknown message");
  }

}

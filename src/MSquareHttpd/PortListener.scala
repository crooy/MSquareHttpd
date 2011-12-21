package MSquareHttpd;
import java.net.InetAddress
import java.nio.channels.SocketChannel

import com.weiglewilczek.slf4s.Logging

import MSquareHttpd._
/**
 * A producing coroutine that listens to a port and emits
 * connected sockets.
 */
class PortListener(val localAddress: InetAddress, 
                    val port : Int) extends Producer[SocketChannel] with Logging
{
  // Non-blocking server socket:
  private var listener : java.nio.channels.ServerSocketChannel = null  ;

  /**
   Opens the port and loops forever, emitting newly connected sockets.
   */

  override def init () {
    logger.debug("PortListener initializing...") ;

    var client  : java.nio.channels.SocketChannel = null ;
    
    this.listener = java.nio.channels.ServerSocketChannel.open() ;
    listener.socket().bind(new java.net.InetSocketAddress(localAddress, 
                                                          port)) ;
    
    listener.configureBlocking(true) ;
    
    while (true) {
      client = listener.accept() ;

      if (client != null) {
        client.socket() ;
        client.configureBlocking(false) ;
        
        logger.debug(" * Accepted client") ;

        this.send(client);
      }
    }
  }
}




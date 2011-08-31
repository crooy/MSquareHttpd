package MSquareHttpd;
/**
 Represents an open HTTP connection coming from a client.
 */
import java.nio.channels.SocketChannel
import java.io.IOException
import java.nio.ByteBuffer
import com.weiglewilczek.slf4s.Logging


class ClientConer


class ClientConnection (val httpd : M2HTTPD, 
                        val socketChannel : SocketChannel)  extends Logging
{
  
  private var _isOpen = true

  /**
   Is this connection currentnly open?
   */
  def isOpen = _isOpen

  private var requestParser : HTTPRequestParser = new HTTPRequestParser


  /**
   Closes the connection.
   */
  def close () {
    _isOpen = false ;
    socketChannel.close() ;
  }


  /**
   Has this connection parsed a request?
   */
  def hasRequest = (requestParser != null) && requestParser.parsed


  /**
   Produces the request parsed by this parser.
   */
  def takeRequest () : Request = {
    val req = requestParser.toRequest(this)
    // TODO: Needs to be a seconday request parser
    // for Connection: Keep-Alive
    requestParser = null ;
    req
  }

  /**
   Stores input during reads.
   */
  private val readBuffer = ByteBuffer.allocate(8192);

  /**
   Reads more input, if any, and passes it to the http request parser.
   */
  def readMoreInput () {
    logger.debug(" ** Connection has input.") ;
    
    if (!_isOpen) 
      throw new IOException("Cannot read a closed connection!") ;

    this.readBuffer.clear() ;
    
    var numRead = -1 ;

    try {
      numRead = socketChannel.read(this.readBuffer) ;
    } catch {
      case (ioe : IOException) => { 
        this.close() 
        throw ioe
      }
    }

    if (numRead == -1) {
      this.close() 
      throw new IOException("Remote entity shut down socket (cleanly).") ;
    }

    val bytes = this.readBuffer.array() ;

    for (i <- 0 until numRead) {
      requestParser.consume(bytes(i).asInstanceOf[Char]) ;
    }
  }

  /**
   A list of buffers that need to be written.
   */
  private val writeMutex = new Object() 
  private var writeBufferList : List[ByteBuffer] = List() 


  /**
   Queues a reply to send to this connection.

   <code>flushWriteBuffers</code> must be called until it returns true to 
   guarantee the reply has been sent.

   */
  def send (reply : Reply) {
    writeBufferList = reply.headerByteBuffer :: reply.dataByteBuffer :: writeBufferList
  }


  /**
   Sends as much data in its buffer queue as it can without blocking.

   @return True if all data has been sent; false if not.
   */
  def flushWriteBuffers() : Boolean = {
    if (!_isOpen) {
      System.err.println ("ERROR: Attempt to write to closed channel.")
      return true ;
    }

    if (!writeBufferList.isEmpty) {
      if (writeBufferList.head.remaining() > 0) {
        try {
          socketChannel.write(writeBufferList.head) ;
        } catch {
          case (ioe : IOException) => { 
            this.close() ;
            throw ioe
          }
        }
      }
      if (writeBufferList.head.remaining() == 0) {
        writeBufferList = writeBufferList.tail        
      }
    }
    return writeBufferList isEmpty ;
  }
}
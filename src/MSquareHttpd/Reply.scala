package MSquareHttpd;
/**
 A reply to an HTTP request.
 */
import java.nio.ByteBuffer

import MSquareHttpd._
case class Reply (val req : Request, 
                  var code : Int,
                  var data : TypedData)
{
  def this (req : Request) = this (req, 404, "File not found")

  /**
   Maps a reply header to its value.
   */
  val headers = scala.collection.mutable.HashMap[String,String]() ;

  headers("Server") = "Apache/1.3.3.7 (Unix)  (Red-Hat/Linux)" ;  
  headers("Connection") = "Close" ;

  /**
   Renders the headers for an HTTP/1.1-compliant reply.
   */
  private def headerString () : String = {
    val s = new StringBuilder
    s.append("HTTP/1.1 " + code + " " + HTTPCodeName(code)) ;
    for ((h,v) <- headers) {
      s.append(h + ": " + v + "\r\n") 
    }
    s.append("\r\n") ;
    return s.toString
  }

  /**
   Contains the data to be sent to the client.
   */
  lazy val dataByteBuffer : ByteBuffer = 
    data.asByteBuffer

  /**
   Contains the header to be sent to the client.
   */
  lazy val headerByteBuffer : ByteBuffer = {
    val length = dataByteBuffer.capacity

    if (data.contentType != "unknown/unknown") 
      // If you don't know the content type, let the browser guess.
      headers("Content-Type") = data.contentType

    headers("Content-Length") = String.valueOf(length)
    headers("Date") = Time.asHTTPDate()
    
    val head = headerString ()
    
    ByteBuffer.wrap(head.getBytes("UTF-8"))
  }
}
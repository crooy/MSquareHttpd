package MSquareHttpd;
/**
 Typed data has a mime type and can be rendered as a byte buffer.
 */
import java.nio.ByteBuffer

/**
 Transparently converts Scala data into typed data.
 */
object TypedData {
  implicit def stringToTypedData(string : String) = new TypedData {
    def contentType = "text/html" 
    def asByteBuffer = ByteBuffer.wrap(string.getBytes("UTF-8"))
  }
}

trait TypedData {
  def contentType : String ;
  def asByteBuffer : ByteBuffer ;
}

/**
 Raw typed data is a content type plus a byte buffer.
 */
class RawTypedData(val contentType : String, byteBuffer : ByteBuffer) extends TypedData {
  def asByteBuffer = byteBuffer.asReadOnlyBuffer
}



 
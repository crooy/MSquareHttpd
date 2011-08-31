package MSquareHttpd;
import java.io.File
import java.nio.channels.FileChannel


/**
 Handles HTTP requests.

 Request handlers are designed to compose, to support, for example,
 routing handlers or session manager handlers or reformatting
 handlers.

 */
abstract class RequestHandler {

  /**
   If a request has already been handled by other handlers,
   their composed reply, if any, is provided.
   */
  def apply (req : Request, reply : Option[Reply]) : Option[Reply] ; 

  /**
   Composes two request handlers.
   */
  def ==> (handler : RequestHandler) {
    var that = this ;
    new RequestHandler {
      def apply (req : Request, reply : Option[Reply]) : Option[Reply] = {
        val firstReply = that(req,reply)
        handler(req,firstReply)
      }
    }
  }
}

/**
 Always sends 404 File Not Found.
 */
object EmptyRequestHandler extends RequestHandler {
  def apply(req : Request, reply : Option[Reply]) : Option[Reply] = {
    val notFoundReply = new Reply(req,404, "File not found")
    Some(notFoundReply)
  }
}


/**
 Serves as a (very) basic file server.
 */
class SimpleFileRequestHandler (var docRoot : String) extends RequestHandler {
  def apply(req : Request, reply : Option[Reply]) : Option[Reply] = {
    val filePath = docRoot + "/" + req.resource ;
    val file = new File(filePath)

    if (!file.exists) 
      // Send a 404.
      return EmptyRequestHandler(req,reply)

    val fileChannel = (new java.io.RandomAccessFile(filePath, "r")).getChannel() ;
    val size = fileChannel.size() ;
    val byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,0,size) ;
    val mimeType = DefaultMIMETypeMap(req.resource)

    return Some(new Reply(req, 200, new RawTypedData(mimeType, byteBuffer)))
  }
}
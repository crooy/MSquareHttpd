package MSquareHttpd;
/**
 Maps a filename/extension to its MIME type.
 */
class MIMETypeMap {
  private val map = scala.collection.mutable.HashMap[String,String]() 

  map("html") = "text/html" ;
  map("txt") = "text/plain" ;

  map("css") = "text/css" ;
  map("js") = "application/javascript" ;

  map("png") = "image/png" ;
  map("jpg") = "image/jpeg" ;
  map("gif") = "image/gif" ;

  def apply (fileName : String) : String = {
    val parts = fileName.split("[.]") 
    map get (parts(parts.length-1)) match {
      case Some(ty) => ty
      case None => "unknown/unknown"
    }
  }
}

/**
 Maps filenames/extensions to a best guess of their MIME type.
 */
object DefaultMIMETypeMap extends MIMETypeMap 

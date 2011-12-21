package MSquareHttpd;
import java.net.URLDecoder
import scala.collection.mutable.HashMap
import MSquareHttpd._

/**
 Represents an incoming HTTP request.
 */
class Request (val connection : ClientConnection, 
               val method : HTTPMethod,
               val resource : String, 
               val headers : HashMap[String,String],
               val data : String)
{

  /**
   Contains user-definable attributes.
   
   For example, a session-handling request handler could insert session data here.
   */
  val attributes = HashMap[String,Object]()

  private lazy val resourceParts = resource.split("\\?") 

  /**
   Contains the path to the requested resource.
   */
  lazy val path = resourceParts(0) ;  

  /**
   Contains the complete query string for the request.
   */
  lazy val queryString = if (resourceParts.length == 2) resourceParts(1) else "" ;

  /**
   Contains the components of the path to the requested resource.
   */
  lazy val pathComponents = path.split("//+")

  /**
   @return A map representing a query string of the form "param1=value1&param2=value2&..." 
   */
  private def queryStringToMap (queryString : String) : Map[String,String] = {
    val queryStrings = queryString.split("&")
    if (queryString.length > 0) {
      Map() ++ (for (param <- queryStrings) yield {
        val keyValue = param.split("=") ;
        keyValue match {
          case Array(key,value) => (URLDecoder.decode(key,"UTF-8"),
                                    URLDecoder.decode(value,"UTF-8"))
          case Array(key) => (URLDecoder.decode(key,"UTF-8"),"true")
        }
      })
    } else {
     return Map()
    }
  }
  
  /**
   Maps a parameter name to its value.

   If the parameter was not assigned, its value is "true".
   */
  lazy val query : Map[String,String] = queryStringToMap(queryString)
    
    
  /**
   Maps a POST parameter to its value.
   */
  lazy val posts : Map[String,String] = 
    (headers get "Content-Type") match {
      case Some("application/x-www-form-urlencoded") => queryStringToMap(data)
      case None => Map()
    }
  

  /**
   @return A logger.debug-friendly representation of this procedure.
   */
  override def toString : String = {
    "Method: " + method + "\n" +
    "Resource: " + resource + "\n" + 
    "Headers: " + headers + "\n" + 
    "Query: " + query + "\n" +
    "Post: " + posts + "\n" +
    "Data: " + data + "\n" ;
  }
}
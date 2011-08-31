package MSquareHttpd;
import scala.collection.mutable.HashMap

/**
 A continuation-based HTTP request parser.
 
 The parser consumes one character at a time,
 which means that the parsing process can be suspended at any time.

 At the moment, this does not support Keep-Alive HTTP connections.
 */
class HTTPRequestParser {

  /**
   Indicates the current position of the parser.
   */
  private trait ParsingState ;
  private case class FirstLine(val line : String) extends ParsingState 
  private case class HeaderLine(val line : String) extends ParsingState 
  private case class NextLine(val line : String) extends ParsingState
  private case class Data(val data : StringBuilder) extends ParsingState
  private case object End extends ParsingState
  private case object EndHeaders extends ParsingState

  /**
   Marks the current state of the parser.
   */
  private var state : ParsingState = FirstLine("") ;


  /* Components of the request. */
  private var method : HTTPMethod = null ;
  private var resource : String = null ;

  private var headers : HashMap[String,String] = HashMap() ;

  private var data : String = null ;
  
  private var contentLength : Int = -1 ;

  /**
   Has a complete request been parsed?
   */
  def parsed = state == End || ((method == GET) && state == EndHeaders)

  private def processFirstLine(line : String) {
    val parts = line.split(" ") ;
    parts(0) match {
      case "GET" => this.method = GET ;
      case "POST" => this.method = POST ;
      case _ => throw new Exception("Unknown HTTP method: " + parts(0))
    }
    resource = parts(1) ;
  }

  private def processHeaderLine(line : String) {
    val parts = line.split(": ") ;
    //? not sure what this is suppose to do
    //headers = (headers(parts(0)) = parts(1)) ;
    
    headers get ("Content-Length") match {
      case Some(l) => {
        contentLength = Integer.parseInt(l) 
      }
      case None => {}
    }
  }


  /**
   Update the state of the parser with the next character.
   */
  def consume (c : Char) {
    state =
      (state,c) match {
        case (FirstLine(ln),'\r'|'\n') => { processFirstLine(ln) ; NextLine(""+c) }
        case (FirstLine(ln),_) => FirstLine(ln + c)

        case (NextLine("\n"),'\n') => EndHeaders
        case (NextLine("\r\n\r"),'\n') => EndHeaders
        case (NextLine("\r"),'\n') => NextLine("\r\n") 
        case (NextLine("\r\n"),'\r') => NextLine("\r\n\r") 

        case (NextLine(_), c) => HeaderLine(String.valueOf(c)) 
        
        case (HeaderLine(ln),'\r'|'\n') => { processHeaderLine(ln) ; NextLine(""+c) }
        case (HeaderLine(ln),_) => HeaderLine(ln + c)
        
        case (EndHeaders,c) if contentLength > 1 => { 
          val sb = new StringBuilder() ;
          sb.append(c) ;
          Data(sb) ;
        }

        case (Data(sb),c) if contentLength > (sb.length + 1) => {
          sb.append(c) ;
          Data(sb)
        }

        case (Data(sb),c) => {
          sb.append(c)
          this.data = sb.toString
          End
        }
          
        
        case _ => throw new Exception(state + "/" + c + "\n\n" + headers) 
      }
  }
  
  /**
   If a request is completed, produce the Request object.
   */
  def toRequest(conn : ClientConnection) : Request = {
    if (this.parsed) 
      new Request(conn,method,resource,headers,data)
    else 
      throw new Exception("HTTP request not completed!")
  }
}
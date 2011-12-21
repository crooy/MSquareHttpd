package MSquareHttpd;
import com.weiglewilczek.slf4s.Logging

import MSquareHttpd._

/**
 A coroutine that consumes HTTP requests and produces HTTP replies. 
 */
class RequestProcessor extends Transducer[Request,Reply] with Logging {

  private val farm = new ThreadFarm(1024,10)

  override def init () {
    farm.start();
  }
  
  def receive(mesg:Message[Request]){
    handleRequest(mesg.get()) ;
  }
  
  /**
   The host router determines which request handler should be used for the 
   host specified in the request.

   The default hostRouter produces 404s for every request.
   */
  var hostRouter : HostRouter = 
    EmptyHostRouter ;
  
  def handleRequest(req:Request) = {
    farm run {
        var handler : RequestHandler = null 
        try {
          handler = hostRouter(req.headers("Host"))
          
          // If the handler produces a reply, pass it on.
          handler(req,None) match {
            case Some (reply) => send(reply)
            case None => ()
          }
        } catch {
          case ex => {
            System.err.println("error:\n" + ex)
            val reply = new Reply(req,500,"Internal server error")
            send(reply)
          }
        }
      }
  }
  
}
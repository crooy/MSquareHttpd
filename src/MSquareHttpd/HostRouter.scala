package MSquareHttpd;
import scala.collection.immutable.HashMap


/**
 A host router selects a request handler based on the virtual host.
 */
abstract class HostRouter {
  val fallback : HostRouter = EmptyHostRouter ;
  def apply (host : String) : RequestHandler ;
}

/**
 Always returns the <code>EmptyRequestHandler</code>.
 */
object EmptyHostRouter extends HostRouter {
  def apply (host : String) : RequestHandler = EmptyRequestHandler ;
}


/**
 Always returns the supplied request handler, regardless of host.
 */
class DefaultHostRouter (defaultHandler : RequestHandler) extends HostRouter {
  def apply (host : String) = defaultHandler
}


/**
 Maintains a hash table of virtual hosts and their associated request handlers.
 */
class HashtableHostRouter (override val fallback : HostRouter) extends HostRouter {
  def this () = this (EmptyHostRouter)

  private val tableMutex = new Object() 
  @volatile private var table = 
    HashMap[String,RequestHandler]()

  /**
   @return The request handler if it exists, or else looks it up in the fallback host router.
   */
  def apply(host : String) : RequestHandler = try {
    table.getOrElse (host, fallback(host))
  }

  /**
   Installs a request handler at the specified host.
   */
  def update (host : String, handler : RequestHandler) {
    tableMutex synchronized {
      table = table + (host -> handler)
    }
  }
}
package MSquareHttpd
/**
 * Represents an HTTP request method.
 */
trait HTTPMethod

case object POST extends HTTPMethod
case object GET extends HTTPMethod
case object PUT extends HTTPMethod
case object DELETE extends HTTPMethod


package MSquareHttpd
/**
 * Converts HTTP status codes into their corresponding string.
 *
 * For example, 200 becomes "OK" and 404 becomes "Not Found".
 */
object HTTPCodeName {

  private var table = new Array[String](1000)

  table(200) = "OK"
  table(400) = "Bad Request"
  table(404) = "Not Found"

  def apply(code: Int): String = table(code)
}
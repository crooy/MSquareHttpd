package MSquareHttpd;
/**
 Provides convenience functions for manipulating times.
 */
object Time {

  /**
   The current time is milliseconds.
   */
  def now () : Long = new java.util.Date().getTime()
  
  private val gmtFormat = 
    new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", 
                                   java.util.Locale.US);

  gmtFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));


  /**
   The current time rendered in the format required by the HTTP specification.
   */
  def asHTTPDate() : String = gmtFormat.format(new java.util.Date());

  /**
   A time rendered in the format required by the HTTP specification.
   */
  def asHTTPDate(time : Long) = gmtFormat.format(time) ;

  
  /**
   A time a now + offset ms into the future in the format required by the HTTP specification.

   Useful for setting cache expiration times.
   */
  def asFutureHTTPDate(offset : Long) = 
    gmtFormat.format(new java.util.Date().getTime() + offset);
}

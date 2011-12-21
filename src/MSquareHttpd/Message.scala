package MSquareHttpd

sealed trait Message[O] {
  def get(): O;
}
case class AnyMessage[O](wrappedObject: O) extends Message[O] {
  def get(): O = wrappedObject;

}

object Message {
  implicit def newMessage[O](obj: O): AnyMessage[O] = new AnyMessage[O](obj);
}


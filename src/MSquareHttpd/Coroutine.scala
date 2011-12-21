package MSquareHttpd
import akka.actor.Actor
import MSquareHttpd._

/**
 * A coroutine is a process (in this case a thread) that communicates with other coroutines.
 */
trait Coroutine {
  def startup():Unit;
  def init(){}
}


class CoroutineActor[X](receiver:Message[X]=>Unit) extends Actor{
  def receive = {
    case mesg:Message[X] => receiver(mesg)
    case _ => throw new RuntimeException("unknown message");
  }
}
package MSquareHttpd
import akka.actor.Actor

/**
 * An I-consumer is a coroutine that consumes type-I objects.
 */

trait Consumer[I] extends Coroutine{
	val myActor = Actor.actorOf(new CoroutineActor(receive _));
	def ! (msg:Message[I]) = {
	  myActor ! msg;
	}
	override def startup {
	  myActor.start();	  
	}
	def receive(mesg:Message[I]);
	
}

package MSquareHttpd
/**
 * An I,O-transducer consumes type-I objects and produces type-O objects.
 */
trait Transducer[I, O] extends Consumer[I] with Producer[O]{
	override def startup {
	  receivers.map((r)=>r.startup());
	  myActor.start();
	}
}

package MSquareHttpd
/**
 * A O-producer is a coroutine that produces type-O objects for consumption by other coroutines.
 */
trait Producer[O] extends Coroutine {
  var receivers:List[Consumer[O]] = Nil;
  
  private def register(consumer:Consumer[O]){
    receivers = consumer::receivers;
  }
  
  override def startup {
	receivers.map((r)=>r.startup());
  }
  
  /**
   * Composes this producing coroutine with a transducing coroutine.
   *
   * @return A fused producing coroutine.
   */
  def ==>[O2](transducer: Transducer[O, O2]): Producer[O2] = {
    register(transducer);
    var me = this;
    new Producer[O2]{            
      override def startup(){
        transducer.startup();
        me.startup();
      }
    }
  }

  /**
   * Composes this producing coroutine with a consuming coroutine.
   *
   * @return A fused coroutine.
   */
  def ==>(consumer: Consumer[O]): Coroutine = {
    register(consumer);
    var me = this;
    new Coroutine{
      override def startup{
        consumer.startup();
        me.startup();
      }
    }
  }
  
  def send (message: Message[O]){
    receivers.map((a)=>a!message)
  }

}

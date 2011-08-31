package MSquareHttpd;
import scala.actors.threadpool.LinkedBlockingQueue

/**
 Manages a stable of threads, ready to execute tasks concurrently.
 */
class ThreadFarm (val queueSize : Int, val numberThreads : Int) {

  private val tasks = 
    new LinkedBlockingQueue[() => Unit] (queueSize) ;


  /**
   Each work pulls a task and executes it, looping forever.
   */
  private object Worker extends Runnable {
    def run () {
      while (true) {
        try {
          val task = tasks.take()
          task()
        } catch {
          case (ex : Exception) => {
            ex.printStackTrace() ;
          }
        }
      }
    }
  }

  /**
   Starts the stable.
   */
  def start () {
    for (i <- 1 to numberThreads) {
      val workerThread = new Thread(Worker)
      workerThread.start()
    }
  }

  /**
   Accepts an action to be run concurrently.
   */
  def run (action : => Unit) {
    tasks.put(() => action)
  }

  /**
   Accepts a taks to be run concurrently.
   */
  def addTask (task : () => Unit) {
    tasks.put(task)
  }

}
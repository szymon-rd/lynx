package lynx.exec

import java.util.concurrent.Executor
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ArrayBlockingQueue

trait CanYield {
  def yieldNow(): Unit
}

class Scheduler {

  val threadLocalVirt = new ThreadLocal[Thread]()
  private val executor = new Executor {
    override def execute(runnable: Runnable): Unit = {
      submitToWorker{ () => runnable.run()}
    }
  }

  private val nWorkers = 16
  private val workers = (0 until nWorkers).map(new Worker(_, this))
  workers.foreach(_.go())

  private val constrHandle: MethodHandle = {
    val virtualThreadClass = Class.forName("java.lang.VirtualThread")
    val constr = virtualThreadClass.getDeclaredConstructor(classOf[Executor], classOf[String], classOf[Int], classOf[Runnable])
    constr.setAccessible(true)
    val lookup = MethodHandles.lookup()
    lookup.unreflectConstructor(constr)
  }

  
  val currentWorkerAtomic: AtomicInteger = new AtomicInteger(0)
  val vThreadsCounter: AtomicInteger = new AtomicInteger(0)
  def currentWorker() = currentWorkerAtomic.incrementAndGet() % nWorkers

  private def submitToWorker(fn: () => Unit): Unit = {
    val cw = currentWorker()
    workers(cw).submitFn(fn)
  }

  def run(f: CanYield => Unit) = submitToWorker { () =>
    val t = constrHandle.invoke(executor, s"VirtualThread ${vThreadsCounter.getAndIncrement()}", 0, new Runnable {
      override def run(): Unit = {
        object yielding extends CanYield {
          def yieldNow(): Unit = 
            Thread.`yield`()
        }
        f(yielding)
      }
    }).asInstanceOf[Thread]
    t.start()
  }
  
}

class Worker(id: Int, scheduler: Scheduler) {

  val queue = new ArrayBlockingQueue[() => Unit](8 * 1024)

  def submitFn(f: () => Unit): Unit = {
    queue.put(f)
  }

  def go(): Unit = {
    Thread.ofPlatform().name("Worker " + id).start { () =>
      while (true) {
        val f = queue.take()
        try {
          f()
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }
    }
  }
}

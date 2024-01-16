package lynx.exec

import lynx.*
import scala.annotation.tailrec
import lynx.exec.Scheduler
import scala.collection.mutable.ListBuffer

class SchedulerTest extends munit.FunSuite {
  test("Foo") {
    val debug = true
    var output: ListBuffer[String] = ListBuffer.empty

    def printlnWithThread(s: String): Unit = {
     if(debug) output.append(s"[${Thread.currentThread().getClass().getSimpleName()}/${Thread.currentThread().getName()}] $s")
    }

    printlnWithThread("Starting")

    val scheduler = new Scheduler()

    def yieldingProgram(id: Int, n: Int, yielding: CanYield): Unit = {
      var i = 0
      while (i < n) {
        i += 1
        if(debug) printlnWithThread("Yielding")
        val a = yielding.yieldNow()
        if(debug) printlnWithThread(s"[$id] Got thread $a")
      }
    }

    def runProgram(id: Int, n: Int): Unit = {
      scheduler.run { (yielding: CanYield) =>
        val timeNow = System.currentTimeMillis()
        yieldingProgram(id, n, yielding)
        val timeAfter = System.currentTimeMillis()
        println(s"[$id] Time taken: ${timeAfter - timeNow}ms")
      }
    }

    (0 to 5).foreach(i => runProgram(i, 100000))
    Thread.sleep(3000)
    println("Output:")
    output.foreach(println)
  }
}

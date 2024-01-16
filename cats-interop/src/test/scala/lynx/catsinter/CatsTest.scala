package lynx.catsinter

import cats.*
import cats.effect.*
import lynx.*
import munit.CatsEffectSuite
import java.nio.file.Files
import java.nio.file.Paths
import language.experimental.captureChecking
import java.nio.file.Path
import java.nio.file.NoSuchFileException

class CatsTest extends CatsEffectSuite {

  test("reflect IO") {
    def ioa: Int requires IO = IO.delayR(5)

    def program: Int requires IO = {
      ioa + ioa
    }

    val result = Lynx[IO].reify(program)

    result.assertEquals(10)
  }

  // from monadic-reflection library
  test("Rate limiting") {
    object RateLimiter {
      
      import cats.effect._
      import cats.syntax.all._
      import cats.effect.std.{ Semaphore }

      import scala.concurrent.duration._

      // original rate limiter code
      def rateLimited[A, B](semaphore : Semaphore[IO], function : A => IO[B]): A => IO[B] = input =>
        for {
          _  <- semaphore.acquire
          timerFiber <- IO.sleep(1.second).start
          result <- function(input)
          _ <- timerFiber.join
          _  <- semaphore.release
          } yield result

      // example translated to direct style
      def rateLimitedDirectStyle[A, B](semaphore : Semaphore[IO], function : A -> B requires IO): (A -> B requires IO) = input => {
        semaphore.acquire.r
        val timerFiber = IO.sleep(1.second).start.r
        val result = function(input)
        timerFiber.join.r
        semaphore.release.r
        result
      }

      // "big" dataset
      val myData : List[Int] = (1 to 30).toList

      def process: List[String] requires IO = {
        println("Starting to process!")
        val sem = Semaphore[IO](10).r
        val limited = rateLimitedDirectStyle(sem, n => { println(s"hey! ${n}"); n.toString })
        // here we need to locally reify, since parTraverse has type:
        //   def parTraverse[A](as: List[A])(f: A => IO[B]): IO[List[B]]
        myData.parTraverse(n => Lynx[IO].reify { limited(n) }).r
      }
    }
    val result = Lynx[IO].reify(RateLimiter.process)
    result.assertEquals(List("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                             "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
                             "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"))
  }
  
  // Doesn't work! Must put in .use of Resource
  test("reflect IO with resource") {
    def ioa: String requires IO = IO.delayR("world")

    def makeFileWithContent(ct: String): Path = {
      val file = Files.createTempFile("test", "txt")
      Files.write(file, ct.getBytes)
      file
    }

    def program: Unit requires IO = {
      val file = Resource.make[IO, Path](IO.delay(makeFileWithContent("hello")))(f => IO.delay { println("Deleting"); Files.delete(f); Thread.sleep(1000)})
      val file1 = file.useR
      intercept[NoSuchFileException] { Files.readString(file1) }
    }

    Lynx[IO].reify(program)
  }

  



}
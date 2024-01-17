package lynx.catsinter

import cats.*
import cats.effect.*
import lynx.*
import munit.CatsEffectSuite
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.NoSuchFileException
import java.rmi.ServerError

class CatsTest extends CatsEffectSuite {

  test("reflect IO") {
    def ioa: LIO[Int] = IO.delayR(5)

    def program: LIO[Int] = {
      ioa + ioa
    }

    val result = Lynx[IO].reify(program)

    result.assertEquals(10)
  }

  case class Request(path: String, body: String) 
  case class Response(status: Int, body: String) {
    def toJson: String = s"""{"status": ${status}, "response": "${body}"}"""
  }

  trait HttpError
  case object ServerError extends HttpError
  case object ClientError extends HttpError

  class Routes(handers: Map[String, Request => LIO[LEither[HttpError, Response]]]) {
    def run(req: Request): LIO[LEither[HttpError, Response]] = {
      handers.get(req.path) match {
        case Some(handler) => handler(req)
        case None => IO.delayR(Response(404, "Not found"))
      }
    }
    def handleGet(path: String)(handler: Request => LIO[LEither[HttpError, Response]]): Routes = {
      new Routes(handers + (path -> handler))
    }
  }
  object Routes {
    def apply(handers: (String, Request => LIO[LEither[HttpError, Response]])*): Routes = new Routes(handers.toMap)
  }


  test("Async server") {

    def makeRequest(url: String): LIO[LEither[HttpError, Response]] = IO.delayR(Response(200, "Hello from " + url))

    val routes = Routes()
      .handleGet("/hello") { req =>
        IO.delayR(Response(200, "Hello"))
      }
      .handleGet("/proxy") { req =>
        val address = req.body
        val response = makeRequest(address)
        println("Got response with status " + response.status)
        Response(200, response.toJson)
      }
      .handleGet("/proxyBatch") { req =>
        val addresses = req.body.split("\n")
        val responses = addresses.map(makeRequest)
        Response(200, s"""{"responses": [${responses.map(_.toJson).mkString(",")}]}""")
      }


      def reify[A](program: LIO[LEither[HttpError, A]]): IO[Either[HttpError, A]] = {
        type EitherHttp[A] = Either[HttpError, A]
        Lynx[IO].reify(Lynx[EitherHttp].reify(program))
      }
      reify(routes.run(Request("/hello", ""))).assertEquals(Right(Response(200, "Hello")))
      reify(routes.run(Request("/proxy", "http://google.com")))
        .assertEquals(Right(Response(200, """{"status": 200, "response": "Hello from http://google.com}"""")))
      reify(routes.run(Request("/proxyBatch", "http://google.com\nhttp://yandex.ru")))
        .assertEquals(Right(Response(200, """{"responses": [{"status": 200, "response": "Hello from http://google.com"},{"status": 200, "response": "Hello from http://yandex.ru"}]}""")))
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
      def rateLimitedDirectStyle[A, B](semaphore : Semaphore[IO], function : A => LIO[B]): (A => LIO[B]) = input => {
        semaphore.acquire.?
        val timerFiber = IO.sleep(1.second).start.?
        val result = function(input)
        timerFiber.join.?
        semaphore.release.?
        result
      }

      // "big" dataset
      val myData : List[Int] = (1 to 30).toList

      def process: LIO[List[String]] = {
        println("Starting to process!")
        val sem = Semaphore[IO](10).?
        val limited = rateLimitedDirectStyle(sem, n => { println(s"hey! ${n}"); n.toString })
        // here we need to locally reify, since parTraverse has type:
        //   def parTraverse[A](as: List[A])(f: A => IO[B]): IO[List[B]]
        // todo: get rid of par traverse
        myData.parTraverse(n => Lynx[IO].reify { limited(n) }).?
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
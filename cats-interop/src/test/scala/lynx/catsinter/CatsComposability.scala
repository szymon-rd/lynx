package lynx.catsinter

import cats.*
import cats.effect.*
import cats.data.*
import lynx.*
import munit.CatsEffectSuite
import scala.concurrent.Future
import scala.util.control.NonFatal

class CatsComposabilityTest extends CatsEffectSuite {

  object DbLib {
    type DbEither[A] = Either[DbError, A]
    def dbLoad(id: Int): Either[DbError, Int] = {
      Either.cond(id > 0, id - 1, SomeError)
    }

    sealed trait DbError extends Exception with Product
    case object SomeError extends DbError
  }

  import DbLib.*

  test("EitherT") {
    def load(id: Int): IO[Either[DbError, Int]] =
      IO.delay(DbLib.dbLoad(id))

    val loadAll: EitherT[IO, DbError, (Int, Int, Int)] =
      for {
        i1 <- EitherT(load(1))
        i2 <- EitherT(load(i1))
        i3 <- EitherT(load(i2))
      } yield (i1, i2, i3)

    val x: IO[Either[DbError, (Int, Int, Int)]] = loadAll.value
    x.assertEquals(Left(SomeError))
  }

  test("Lynx requiresN") {

    def load(id: Int): Int requiresN (CapFor[DbEither], CapFor[IO]) = 
      IO.delayR(DbLib.dbLoad(id)).r
    
    def program: (Int, Int, Int) requiresN (CapFor[DbEither], CapFor[IO]) = // or DbEither[(Int, Int, Int)] requires IO
      val i1 = load(1)
      val i2 = load(i1)
      val i3 = load(i2)
      (i1, i2, i3)

    val x: IO[Either[DbError, (Int, Int, Int)]] = Lynx[IO].reify(Lynx[DbEither].reify(program))
    x.assertEquals(Left(SomeError))

  }

  test("Lynx requiresN polymorphic") {

    trait DbLoader[F[_]] {
      def load(id: Int): DbEither[Int] requires F
    }

    object AsyncLoader extends DbLoader[IO] {
      def load(id: Int): DbEither[Int] requires IO = 
        IO.delayR(DbLib.dbLoad(id))
    }

    def load[F[_]](loader: DbLoader[F])(id: Int): Int requiresN (CapFor[DbEither], CapFor[F]) = 
      loader.load(id).r
      

    def loadN[F[_]](loader: DbLoader[F]): (Int, Int, Int) requiresN (CapFor[DbEither], CapFor[F]) =
      val loaderLoad = load(loader)
      val i1 = loaderLoad(1) // no need for EitherT
      val i2 = loaderLoad(i1)
      val i3 = loaderLoad(i2)
      (i1, i2, i3)
    
    def program: (Int, Int, Int) requiresN (CapFor[DbEither], CapFor[IO]) =
      loadN(AsyncLoader)
  
    val x = Lynx[IO].reify(Lynx[DbEither].reify(program))
    x.assertEquals(Left(SomeError))

  }





}
package lynx.catsinter

import language.experimental.captureChecking
import language.experimental
import cats.*
import cats.effect.*
import lynx.*

class CatsMonadic[M[+_]: Monad] extends Monadic[M] {
  def pure[A](a: A) = 
    Monad[M].pure(a)
  def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] =
    Monad[M].flatten(Monad[M].tailRecM(init) { mx => Monad[M].map(mx)(f) })
}

given [M[+_]](using M: Monad[M]): Monadic[M] = CatsMonadic()

extension [M[+_], R](mr: M[R])(using M: Monadic[M])
  inline def pureR[A](a: A): A requires M = M.pure(a).reflect

extension [A](io: IO.type)
  inline def delayR(f: => A): A requires IO = io(f).reflect

type CatsResource[M[_], R] = CanReflect[Resource[M, Any]] ?=> R

extension [M[+_] : Monad, R](resource: Resource[M, R]^)
  def useR(using M: MonadCancel[M, Throwable]): (R requires M)^{resource} = resource.use[R](a => M.pure(a))(M).reflect

type LIO[A] = A requires IO
type LEither[L, +R] = R requires ([A] =>> Either[L, A])
object LEither {
  def left[L, R](l: L): LEither[L, R] = Lynx[[A] =>> Either[L, A]].reflect(Left(l))
  def right[L, R](r: R): LEither[L, R] = Lynx[[A] =>> Either[L, A]].reflect(Right(r))
}
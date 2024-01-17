package lynx.data

import scala.annotation.tailrec
import lynx.*

type OptionalM[A] = Option[A]
type Optional[A] = CanReflect[OptionalM[Any]] ?=> A

class OptionalMonadic extends Monadic[OptionalM] {

  type M[A] = Option[A]

  def pure[A](a: A): M[A] = Option(a)

  def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] =
    @tailrec
    def sequenceRec(c: M[X]): M[R] = {
      val next = init.map(f)
      next match {
        case None => None
        case Some(Left(mx)) => sequenceRec(mx)
        case Some(Right(res)) => res
      }
    }
    sequenceRec(init)

  def use[A](fn: M[A]): Optional[A] = reflect(fn)

  def some[A](a: A): Optional[A] = reflect(Option(a))
  def none[A]: Optional[A] = reflect(None)
  def orElse[A](a: Optional[A], b: => A): Optional[A] = reflect(reify(a).orElse(Some(b)))
}

val Optional = new OptionalMonadic

package lynx.exec

import lynx.*

type AsyncM[A] = () => A
type Async[A] = CanReflect[AsyncM[Any]] ?=> A


class AsyncMonadic extends Monadic[AsyncM] {
  type M[A] = AsyncM[A]
  
  def pure[A](a: A): M[A] = () => a

  def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] = {
    def sequenceRec(m: M[X]): M[R] = {
      val next = f(m())
      next match {
        case Left(mx) => sequenceRec(mx)
        case Right(res) => res
      }
    }
    sequenceRec(init)
  }
}
package lynx.data

import lynx.*
import scala.annotation.tailrec

type StateM[S, +A] = (S) => (S, A)
type StatesM[S] = [A] =>> StateM[S, A]

type State[S, A] = CanReflect[StateM[S, Any]] ?=> A
type States[S] = [A] =>> State[S, A]

class StateMonadic[S] extends Monadic[StatesM[S]] {
  type M[A] = (S) => (S, A)

  def pure[A](a: A): M[A] = s => (s, a)

  def map[A, B](m: M[A])(f: A => B): M[B] = s => {
    val (newS, a) = m(s)
    (newS, f(a))
  }

  def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] =
    @tailrec
    def sequenceRec(state: S, m: M[X]): (S, R) = {
      val (newState, x) = m(state)
      f(x) match {
        case Left(mx) => sequenceRec(newState, mx)
        case Right(res) => res(newState)
      }
    }
    s => sequenceRec(s, init)

  def use[A](fn: (S) => (S, A)): State[S, A] = reflect(fn)
  def get(): State[S, S] = reflect(s => (s, s))
  def set(s: S): State[S, Unit] = reflect(_ => (s, ()))
  def update(fn: S => S): State[S, Unit] = reflect(s => (fn(s), ()))
}

def State[S] = new StateMonadic[S]
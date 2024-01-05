package com.virtuslab.lynx.data

import com.virtuslab.lynx.*

type StateM[S, A] = (S) => (S, A)
type StatesM[S] = [A] =>> StateM[S, A]

type State[S, A] = CanReflect[StatesM[S]] ?=> A
type States[S] = [A] =>> State[S, A]

class StateMonadic[S] extends Monadic[StatesM[S]] {

  type M[A] = (S) => (S, A)

  def pure[A](a: A): M[A] = s => (s, a)

  // Note that for simplicity this is NOT a tail recursive implementation and will stack overflow!
  def sequence[X, R](init: M[X])(f: X => Either[M[X], M[R]]): M[R] =
    s => init(s) match {
      case (newState, x) =>
        f(x) match {
          case Left(mx) => sequence(mx)(f)(newState)
          case Right(res) => res(newState)
        }
    }

  def use[A](fn: (S) => (S, A)): State[S, A] = reflect(fn)
  def get(): State[S, S] = reflect(s => (s, s))
  def set(s: S): State[S, Unit] = reflect(_ => (s, ()))
  def update(fn: S => S): State[S, Unit] = reflect(s => (fn(s), ()))
}

def State[S] = new StateMonadic[S]
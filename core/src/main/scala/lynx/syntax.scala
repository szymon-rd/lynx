package lynx

import language.experimental.captureChecking

infix type requires[A, M[_]] = CanReflect[M[Any]]^ ?-> A
type CapFor[M[_]] = CanReflect[M[Any]]^
infix type requiresN[A, xs] = xs match {
  // tuple cons match
  case EmptyTuple => A
  case CanReflect[m] *: tail => CanReflect[m]^ ?-> requiresN[A, tail]
}


extension [M[+_], R](mr: M[R])
  inline def reflect(using r: CanReflect[M[Any]]): R = r.reflect(mr)
  inline def r(using r: CanReflect[M[Any]]): R = r.reflect(mr)


inline def Lynx[M[+_]: Monadic]: LynxSyntax[M] = LynxSyntax()

case class LynxSyntax[M[+_]]()(using M: Monadic[M]) {
  inline def reify[R](prog: CanReflect[M[Any]]^ ?-> R): M[R] = M.reify[R] { prog }
  inline def reflect[R](mr: M[R]): R requires M = summon[CanReflect[M[Any]]].reflect(mr)
}

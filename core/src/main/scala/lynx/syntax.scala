package lynx

import language.experimental.captureChecking

infix type requires[A, M[_]] = CanReflect[M]^ ?-> A

extension [M[_], R](mr: M[R])
  inline def reflect(using r: CanReflect[M]): R = r.reflect(mr)
  inline def r(using r: CanReflect[M]): R = r.reflect(mr)


inline def Lynx[M[_]: Monadic]: LynxSyntax[M] = LynxSyntax()

case class LynxSyntax[M[_]]()(using M: Monadic[M]) {
  inline def reify[R](prog: CanReflect[M]^ ?-> R): M[R] = M.reify[R] { prog }
  inline def reflect[R](mr: M[R]): R requires M = summon[CanReflect[M]].reflect(mr)
}

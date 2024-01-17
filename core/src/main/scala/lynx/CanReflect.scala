package lynx

import scala.annotation.implicitNotFound

@implicitNotFound("This expression requires the capability for ${M}\nbut cannot find an instance of CanReflect[${M}] in the current scope.\n\nMaybe you forgot to wrap this expression in a call to:\n    ${M}.reify { EXPR }")
trait CanReflect[M] {
  def reflect[N[+_] <: M, R](mr: N[R]): R
}
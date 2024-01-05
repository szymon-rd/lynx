package com.virtuslab.lynx

type requires[A, M[_]] = CanReflect[M] ?=> A


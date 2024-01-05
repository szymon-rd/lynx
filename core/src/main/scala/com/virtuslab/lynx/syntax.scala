package com.virtuslab.lynx

type require[A, M[_]] = CanReflect[M] ?=> A


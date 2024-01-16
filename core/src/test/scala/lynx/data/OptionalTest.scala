package lynx.data

import scala.annotation.tailrec
import lynx.*

class OptionalTest extends munit.FunSuite {

  test("Optional") {
    def add(a: Optional[Int], b: Optional[Int]): Optional[Int] = 
      a + b

    assert(Optional.reify(add(1,2)) == Some(3))
    assert(Optional.reify(add(1,Optional.some(2))) == Some(3))
    assert(Optional.reify(add(1,Optional.none)) == None)
  }

  test("Optional.orElse") {
    def addWithFallback(a: Optional[Int], b: Optional[Int]): Optional[Int] = 
      a + Optional.orElse(b, 0)

    assert(Optional.reify(addWithFallback(1,2)) == Some(3))
    assert(Optional.reify(addWithFallback(1,Optional.some(2))) == Some(3))
    println(Optional.reify(addWithFallback(1,Optional.none)))
    assert(Optional.reify(addWithFallback(1,Optional.none)) == Some(1))
    
  }
}
    


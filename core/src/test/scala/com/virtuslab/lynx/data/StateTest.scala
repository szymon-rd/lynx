package com.virtuslab.lynx.data

class StateTest extends munit.FunSuite {

  test("nextInt") {
    def nextInt(): State[Int, Int] = {
      State[Int].reflect { (s: Int) => (s + 1, s) }
    }

    def program(): State[Int, (Int, Int, Int)] = {
      val s1 = nextInt()
      val s2 = nextInt()
      val s3 = nextInt()
      (s1, s2, s3)
    }

    val (s, (i1, i2, i3)) = State[Int].reify(program())(0)
    assertEquals(s, 3)
    assertEquals(i1, 0)
    assertEquals(i2, 1)
    assertEquals(i3, 2)
  }
}

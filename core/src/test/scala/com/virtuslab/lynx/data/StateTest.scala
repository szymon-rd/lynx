package com.virtuslab.lynx.data

import com.virtuslab.lynx.*

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



  test("userService") {
    case class User(name: String)
    trait UserService[Cap[_]] {
      def addUser(name: String): Unit in Cap
      def getUser(name: String): Option[User] in Cap
    }

    type Storage[A] = StateM[List[User], A]
    case class StateUserService() extends UserService[Storage] {

      override def addUser(name: String): Unit in Storage = {
        println("Adding user: " + name)
        val newUser = User(name)
        State[List[User]].update(newUser :: _)
      }
        
      override def getUser(name: String): Option[User] in Storage =
        println("Getting user: " + name)
        State[List[User]].get().find(_.name == name)
    }

    def program[F[_]](userService: UserService[F]): Option[User] in F = {
      userService.addUser("John")
      userService.addUser("Jane")
      userService.getUser("John")
    }

    val (state, result) = State[List[User]].reify(program(StateUserService()))(List.empty)
    assert(state == List(User("Jane"), User("John")))
    assert(result == Some(User("John")))
  }
}

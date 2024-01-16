package lynx.data

import lynx.*

class CompositionTest extends munit.FunSuite {
  case class User(name: String, age: Int)
  trait UserService[Cap[_]] {
    def addUser(name: String): Unit requires Cap
    def getUser(name: String): Optional[User] requires Cap
  }

  type Storage[A] = StateM[List[User], A]
  case class StateUserService() extends UserService[Storage] {

    override def addUser(name: String): Unit requires Storage = {
      println("Adding user: " + name)
      val newUser = User(name, 21)
      State[List[User]].update(newUser :: _)
    }
      
    override def getUser(name: String): Optional[User] requires Storage =
      println("Getting user: " + name)
      State[List[User]].get().find(_.name == name).reflect
  }

  test("userService") {
    def program[F[_]](userService: UserService[F]): Optional[Int] requires F = {
      userService.addUser("John")
      userService.addUser("Jane")
      val user = userService.getUser("John")
      user.age
    }

    //todo composition
    val (state, result) = State[List[User]].reify(Optional.reify(program(StateUserService())))(List.empty)
    assert(state == List(User("Jane", 21), User("John", 21)))
    assert(result == Some(21))
  }
    
}

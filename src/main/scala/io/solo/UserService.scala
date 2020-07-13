package io.solo

import java.util.UUID.randomUUID

import scala.concurrent.Await
import scala.concurrent.duration._

object UserService {

  private val defaultWaitDuration: Duration = 5 second

  def create(username: String, age: Int): Option[User] = {
    val optUser: Option[User] = Await.result(UserRepository.getByUsername(username), defaultWaitDuration)
    optUser match {
      case Some(_) => None
      case None =>
        val uuid = randomUUID().toString
        Some[User](insert(uuid, username, age))
    }
  }

  private def insert(uuid: String, username: String, age: Int): User = {
    val user = User(uuid, username, age)
    UserRepository.save(user)
    user
  }

  def getAll(sort: Option[String]): Seq[User] = {
    Await.result(UserRepository.getAll(sort), defaultWaitDuration)
  }

  def getById(id: String): Option[User] = {
    Await.result(UserRepository.getById(id), defaultWaitDuration)
  }

  def updateAge(id: String, age: Int): Option[Unit] = {
    getById(id)
      .map(user => User(user.id, user.username, age))
      .map(UserRepository.save)
      .orElse(None)
  }

  def delete(id: String): Option[Unit] = {
    getById(id)
      .map(user => UserRepository.delete(user.id))
      .orElse(None)
  }
}

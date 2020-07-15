package io.solo

import java.util.UUID.randomUUID

import cats.effect.{ContextShift, IO}
import io.solo.UserRoutes.CreateUserDto
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object UserService {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def logger = LoggerFactory.getLogger(this.getClass)

  def create(user: CreateUserDto): IO[Either[String, User]] = {
    logger.info(s"Creating new user $user")
    futureToIo(UserRepository.getByUsername(user.username))
      .flatMap {
        case None => insert(randomUUID().toString, user.username, user.age)
          .map {
            case Some(user) => Right(user)
            case None => Left("User was not created")
          }
        case Some(_) => IO(Left("Username already in use"))
      }
  }

  private def insert(id: String, username: String, age: Int): IO[Option[User]] = {
    val user = User(id, username, age)
    futureToIo(UserRepository.insert(user))
      .map(inserted => if (inserted) Some(user) else None)
  }

  def getAll(sort: Option[String]): IO[Seq[User]] = {
    futureToIo(UserRepository.getAll(sort))
  }

  def getById(id: String): IO[Option[User]] = {
    futureToIo(UserRepository.getById(id))
  }

  def updateAge(id: String, age: Int): IO[Either[String, User]] = {
    logger.info(s"Updating user $id age -> $age")
    getById(id)
      .flatMap {
        case Some(user) => updateAge(user, age)
          .map {
            case Some(user) => Right(user)
            case None => Left("User was not updated")
          }
        case None => IO(Left("User not found"))
      }
  }

  private def updateAge(user: User, age: Int): IO[Option[User]] = {
    futureToIo(UserRepository.updateAge(user, age))
      .map(updated => if (updated) Some(User(user.id, user.username, age)) else None)
  }

  def delete(id: String): IO[Either[String, User]] = {
    logger.info(s"Deleting user $id")
    getById(id)
      .flatMap {
        case Some(user) => futureToIo(UserRepository.delete(user.id))
          .map(deleted => if (deleted) Right(user) else Left("User was not deleted"))
        case None => IO(Left("User not found"))
      }
  }

  private def futureToIo[A](f: Future[A]): IO[A] = {
    IO.fromFuture(IO(f))
  }
}

package io.solo

import cats.effect._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.json4s.native._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.json4s.{DefaultFormats, JValue, Reader, _}

object UserRoutes {

  implicit val formats: DefaultFormats = DefaultFormats

  implicit val createUserReader: Reader[CreateUserDto] = (value: JValue) => value.extract[CreateUserDto]
  implicit val createUserDecoder: EntityDecoder[IO, CreateUserDto] = jsonOf[IO, CreateUserDto]

  implicit val updateUserReader: Reader[UpdateUserDto] = (value: JValue) => value.extract[UpdateUserDto]
  implicit val updateUserDecoder: EntityDecoder[IO, UpdateUserDto] = jsonOf[IO, UpdateUserDto]

  implicit val userListWriter: Writer[Seq[User]] = (obj: Seq[User]) => Extraction.decompose(obj)
  implicit val userListEncoder: EntityEncoder[IO, Seq[User]] = jsonEncoderOf[IO, Seq[User]]

  implicit val userWriter: Writer[User] = (obj: User) => Extraction.decompose(obj)
  implicit val userEncoder: EntityEncoder[IO, User] = jsonEncoderOf[IO, User]

  implicit val errorResponseWriter: Writer[ErrorResponseDto] = (obj: ErrorResponseDto) => Extraction.decompose(obj)
  implicit val errorResponseEncoder: EntityEncoder[IO, ErrorResponseDto] = jsonEncoderOf[IO, ErrorResponseDto]

  case class CreateUserDto(username: String, age: Int)

  case class UpdateUserDto(age: Int)

  case class ErrorResponseDto(message: String)

  object SortQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("sort")

  def route() = {
    HttpRoutes.of[IO] {
      case GET -> Root / "users" :? SortQueryParamMatcher(sort) =>
        Ok(UserService.getAll(sort))
      case req@POST -> Root / "users" =>
        req.decode((createUser: CreateUserDto) => {
          UserService.create(createUser.username, createUser.age)
            .map(user => Ok(user))
            .getOrElse(Conflict(ErrorResponseDto("Username already in use")))
        })
      case GET -> Root / "users" / id =>
        UserService.getById(id)
          .map(user => Ok(user))
          .getOrElse(BadRequest(ErrorResponseDto("User not found")))
      case req@PUT -> Root / "users" / id =>
        req.decode((updateUser: UpdateUserDto) => {
          UserService.updateAge(id, updateUser.age)
            .map(_ => Accepted())
            .getOrElse(BadRequest(ErrorResponseDto("User not found")))
        })
      case DELETE -> Root / "users" / id =>
        UserService.delete(id)
          .map(_ => NoContent())
          .getOrElse(BadRequest(ErrorResponseDto("User not found")))
    }.orNotFound
  }
}

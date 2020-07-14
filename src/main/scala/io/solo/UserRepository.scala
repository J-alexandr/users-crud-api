package io.solo

import com.mongodb.client.model.Filters
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Sorts.{ascending, descending}
import org.mongodb.scala.{Completed, Document, MongoClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.classTag

object UserRepository {

  private val client: MongoClient = MongoClient("mongodb://root:rootpassword@localhost:27017")
  private val usersCollection = client.getDatabase("usersDb").getCollection("users")
  private val codecRegistry: CodecRegistry = fromRegistries(fromProviders(Macros.createCodecProvider[User]()), DEFAULT_CODEC_REGISTRY)

  def insert(user: User): Future[Boolean] = {
    val document: Document = Document(
      "id" -> user.id,
      "username" -> user.username,
      "age" -> user.age
    )

    usersCollection.insertOne(document)
      .toFuture()
      .map {
        case Completed() => true
        case _ => false
      }
  }

  def updateAge(user: User, age: Int): Future[Boolean] = {
    val filter: Document = Document(
      "id" -> user.id
    )
    val document: Document = Document(
      "id" -> user.id,
      "username" -> user.username,
      "age" -> age
    )

    usersCollection.replaceOne(filter, document)
      .toFuture()
      .map(result => result.getModifiedCount > 0)
  }

  def getAll(sort: Option[String]): Future[Seq[User]] = {
    usersCollection.find()
      .sort(getSortingCriteria(sort))
      .toFuture()
      .map(list => for (document <- list) yield convertToUser(document))
  }

  private def getSortingCriteria(sort: Option[String]): conversions.Bson = {
    sort match {
      case Some(s) if s.equalsIgnoreCase("desc") => descending("username")
      case _ => ascending("username")
    }
  }

  def getById(id: String): Future[Option[User]] = {
    getByFilter(Filters.eq("id", id))
  }

  def getByUsername(username: String): Future[Option[User]] = {
    getByFilter(Filters.eq("username", username))
  }

  private def getByFilter(filters: Bson): Future[Option[User]] = {
    usersCollection.find(filters)
      .first()
      .toFuture()
      .map {
        case doc@Document(_) => Some(convertToUser(doc))
        case _ => None
      }
  }

  def delete(id: String): Future[Boolean] = {
    usersCollection.deleteOne(Filters.eq("id", id))
      .toFuture()
      .map(result => result.getDeletedCount > 0)
  }

  private def convertToUser(document: Document): User = {
    val bsonDocument = BsonDocumentWrapper.asBsonDocument(document, DEFAULT_CODEC_REGISTRY)
    val bsonReader = new BsonDocumentReader(bsonDocument)
    val decoderContext = DecoderContext.builder.build
    val codec = codecRegistry.get(classTag[User].runtimeClass)
    codec.decode(bsonReader, decoderContext).asInstanceOf[User]
  }
}

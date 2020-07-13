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
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{Completed, Document, MongoClient, Observer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.classTag

object UserRepository {

  private val client: MongoClient = MongoClient("mongodb://root:rootpassword@localhost:27017")
  private val usersCollection = client.getDatabase("usersDb").getCollection("users")
  private val codecRegistry: CodecRegistry = fromRegistries(fromProviders(Macros.createCodecProvider[User]()), DEFAULT_CODEC_REGISTRY)

  def save(user: User): Unit = {
    val document: Document = Document(
      "id" -> user.id,
      "username" -> user.username,
      "age" -> user.age
    )

    usersCollection.insertOne(document)
      .subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println(s"User inserted: ${result}")
        override def onError(e: Throwable): Unit = println(s"Error inserting user: $e")
        override def onComplete(): Unit = println("Insert completed")
      })
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

  def delete(id: String): Unit = {
    usersCollection.deleteOne(Filters.eq("id", id))
      .subscribe(new Observer[DeleteResult] {
        override def onNext(result: DeleteResult): Unit = println(s"Users deleted: ${result.getDeletedCount}")
        override def onError(e: Throwable): Unit = println(s"Error deleting user: $e")
        override def onComplete(): Unit = println("Delete completed")
      })
  }

  private def convertToUser(document: Document): User = {
    val bsonDocument = BsonDocumentWrapper.asBsonDocument(document, DEFAULT_CODEC_REGISTRY)
    val bsonReader = new BsonDocumentReader(bsonDocument)
    val decoderContext = DecoderContext.builder.build
    val codec = codecRegistry.get(classTag[User].runtimeClass)
    codec.decode(bsonReader, decoderContext).asInstanceOf[User]
  }
}

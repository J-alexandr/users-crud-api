name := "users-crud-api"

version := "0.1"

scalaVersion := "2.12.11"

val http4sVersion = "0.21.6"
val mongoClientVersion = "2.9.0"
val logbackVersion = "1.1.3"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-json4s-native" % http4sVersion,
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoClientVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

scalacOptions += "-Ypartial-unification"
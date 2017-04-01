organization := "com.blogspot.yawar"
name := "backend"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.1"

val Http4sVersion = "0.15.7a"

libraryDependencies ++=
  Seq(
   "ch.qos.logback" % "logback-classic" % "1.2.1",
   "com.h2database" % "h2" % "1.4.194",
   "io.circe" %% "circe-generic-extras" % "0.7.0",
   "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
   "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
   "org.http4s" %% "http4s-circe" % Http4sVersion,
   "org.http4s" %% "http4s-dsl" % Http4sVersion)

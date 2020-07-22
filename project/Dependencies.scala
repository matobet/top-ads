import sbt._

object Dependencies {

  val circeVersion = "0.13.0"
  val fs2Version = "2.4.0"

  val circeDependencies = Seq(
    "circe-core",
    "circe-fs2",
    "circe-generic",
    "circe-generic-extras",
    "circe-parser"
  ).map("io.circe" %% _ % circeVersion)

  val fs2Dependencies = Seq(
    "fs2-core",
    "fs2-io"
  ).map("co.fs2" %% _ % fs2Version)

  val parallelCollections = "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
  val kittens = "org.typelevel" %% "kittens" % "2.1.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.4"
  val catnip = "io.scalaland" %% "catnip" % "1.0.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
}

import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.matobet"
ThisBuild / organizationName := "matobet"

lazy val root = (project in file("."))
  .settings(
    name := "top-ads",
    libraryDependencies ++= (
      circeDependencies ++
      fs2Dependencies ++
      Seq(
        parallelCollections,
        kittens,
        catsEffect,
        catnip,
        scalaTest % Test
      )
    ),
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

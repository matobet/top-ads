package com.github.matobet.top_ads

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, ExitCode, IO, IOApp, Sync}
import cats.implicits._
import cats.derived.cached.monoid
import cats._
import com.github.matobet.top_ads.model._
import io.circe.fs2._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import fs2.{Pipe, Stream, io, text}


object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(impressions, clicks) =>
        process(impressions, clicks).as(ExitCode.Success)
      case _ =>
        IO(printUsage()).as(ExitCode.Error)
    }
  }

  def printUsage(): Unit = {
    println(
      """
        | Usage: top-ads impressions.json clicks.json
        |""".stripMargin)
  }

  def process(impressionsFileName: String, clicksFileName: String): IO[Unit] = {
    Stream.resource(Blocker[IO]).flatMap { implicit blocker =>
      val impressions = jsonStream[Impression](impressionsFileName)
      val clicks = jsonStream[Click](clicksFileName)

      computeMetrics[IO](impressions, clicks).through(writeJson("metrics.json"))
    }.compile.drain
  }

  def computeMetrics[F[_] : Sync](impressions: Stream[F, Impression], clicks: Stream[F, Click]): Stream[F, ImpressionMetrics] = {
    impressions.map(_.toMetrics).foldMonoid.flatMap { impressionMetrics =>
      clicks.fold(impressionMetrics) { _.registerClick(_) }
    }
  }

  def jsonStream[A](fileName: String)(implicit blocker: Blocker, d: Decoder[A]): Stream[IO, A] = {
    io.file.readAll[IO](Paths.get(fileName), blocker, 4096)
      .through(text.utf8Decode)
      .through(stringArrayParser[IO])
      .through(safeDecodeJson[IO, A])
  }

  def writeJson[F[_] : Sync : ContextShift, A](fileName: String)(implicit blocker: Blocker, e: Encoder[A]): Pipe[F, A, Unit] = {
    _.map(_.asJson.noSpaces)
      .through(text.utf8Encode)
      .through(io.file.writeAll(Paths.get(fileName), blocker))
  }

  def safeDecodeJson[F[_] : Sync, A](implicit d: Decoder[A]): Pipe[F, Json, A] = {
    _.flatMap { json =>
      json.as[A].toOption match {
        case Some(x) => Stream.emit(x)
        case None => Stream.empty
      }
    }
  }
}

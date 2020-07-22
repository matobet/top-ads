package com.github.matobet.top_ads

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, Sync}
import io.circe.fs2._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import fs2.{Pipe, Stream, io, text}

object JsonUtils {
  def readStream[F[_] : Sync : ContextShift, A](fileName: String)(implicit blocker: Blocker, d: Decoder[A]): Stream[F, A] = {
    io.file.readAll(Paths.get(fileName), blocker, 4096)
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(safeDecode[F, A])
  }

  def writeStream[F[_] : Sync : ContextShift, A](fileName: String)(implicit blocker: Blocker, e: Encoder[A]): Pipe[F, A, Unit] = {
    _.map(_.asJson.noSpaces)
      .through(text.utf8Encode)
      .through(io.file.writeAll(Paths.get(fileName), blocker))
  }

  def safeDecode[F[_] : Sync, A](implicit d: Decoder[A]): Pipe[F, Json, A] = {
    _.flatMap { json =>
      json.as[A].toOption match {
        case Some(x) => Stream.emit(x)
        case None => Stream.empty
      }
    }
  }
}

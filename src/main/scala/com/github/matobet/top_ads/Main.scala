package com.github.matobet.top_ads

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.github.matobet.top_ads.JsonUtils._
import com.github.matobet.top_ads.model._
import fs2.Stream


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
      val impressions = readStream[IO, Impression](impressionsFileName)
      val clicks = readStream[IO, Click](clicksFileName)
      Metrics.compute[IO](impressions, clicks)
          .observe(_.map(_.appCountryTotals).through(writeStream("metrics.json")))
          .observe(_.map(_.appCountryRecommendations).through(writeStream("recommendations.json")))
          .map(_ => ())
    }.compile.drain
  }
}

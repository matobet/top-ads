package com.github.matobet.top_ads

import java.util.UUID

import cats.effect.{Blocker, IO}
import com.github.matobet.top_ads.model._
import fs2._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MetricsSpec extends AnyWordSpec with Matchers {

  "metrics" should {

    "record impressions" in {
      val impressions = List(
        Impression(UUID.randomUUID(), 0L, "DE", 42L),
        Impression(UUID.randomUUID(), 2L, "CZ", 47L),
        Impression(UUID.randomUUID(), 2L, "CZ", 47L),
        Impression(UUID.randomUUID(), 1L, "DE", 38L),
      )
      val clicks = List()
      computeMetrics(impressions, clicks) shouldBe
        ImpressionMetrics(
          metricsById = impressions.map(i => i.id -> ImpressionMetric(0, 0)).toMap,
          advertisersByImpressions = impressions.map(i => i.id -> i.advertiserId).toMap,
          impressionsByCountryApp = Map(
            AppCountryKey(0L, "DE") -> Set(impressions(0).id),
            AppCountryKey(2L, "CZ") -> Set(impressions(1).id, impressions(2).id),
            AppCountryKey(1L, "DE") -> Set(impressions(3).id)
          )
        )
    }

    "record click revenue" in {
      val impressions = List(
        Impression(UUID.randomUUID(), 0L, "DE", 42L),
        Impression(UUID.randomUUID(), 2L, "CZ", 47L),
        Impression(UUID.randomUUID(), 2L, "CZ", 47L),
        Impression(UUID.randomUUID(), 1L, "DE", 38L),
      )
      val clicks = List(
        Click(impressions(1).id, 0),
        Click(impressions(2).id, 50),
        Click(impressions(2).id, 30),
        Click(impressions(3).id, 10),
        Click(impressions(3).id, 100),
      )
      computeMetrics(impressions, clicks) shouldBe
        ImpressionMetrics(
          metricsById = Map(
            impressions(0).id -> ImpressionMetric(0, 0),
            impressions(1).id -> ImpressionMetric(1, 0),
            impressions(2).id -> ImpressionMetric(2, 80),
            impressions(3).id -> ImpressionMetric(2, 110)
          ),
          advertisersByImpressions = impressions.map(i => i.id -> i.advertiserId).toMap,
          impressionsByCountryApp = Map(
            AppCountryKey(0L, "DE") -> Set(impressions(0).id),
            AppCountryKey(2L, "CZ") -> Set(impressions(1).id, impressions(2).id),
            AppCountryKey(1L, "DE") -> Set(impressions(3).id)
          )
        )
    }
  }

  "compute per app/country totals" in {
    val impressions = List(
      Impression(UUID.randomUUID(), 0L, "DE", 42L),
      Impression(UUID.randomUUID(), 2L, "CZ", 47L),
      Impression(UUID.randomUUID(), 2L, "CZ", 47L),
      Impression(UUID.randomUUID(), 1L, "DE", 38L),
    )
    val clicks = List(
      Click(impressions(1).id, 0),
      Click(impressions(2).id, 50),
      Click(impressions(2).id, 30),
      Click(impressions(3).id, 10),
      Click(impressions(3).id, 100),
      Click(impressions(3).id, 1),
    )
    computeMetrics(impressions, clicks).appCountryTotals.sortBy(_.appID) shouldBe
      List(
        AppCountryTotals(0L, "DE", 1, 0, 0),
        AppCountryTotals(1L, "DE", 1, 3, 111),
        AppCountryTotals(2L, "CZ", 2, 3, 80)
      )
  }

  "compute per app/country recommendations" in {
    val impressions = List(
      Impression(UUID.randomUUID(), 0L, "CZ", 42L),
      Impression(UUID.randomUUID(), 2L, "CZ", 47L),
      Impression(UUID.randomUUID(), 2L, "CZ", 47L),
      Impression(UUID.randomUUID(), 2L, "CZ", 48L),
      Impression(UUID.randomUUID(), 1L, "DE", 47L),
    )
    val clicks = List(
      Click(impressions(0).id, 8),
      Click(impressions(1).id, 20),
      Click(impressions(2).id, 20),
      Click(impressions(3).id, 10),
      Click(impressions(3).id, 11),
      Click(impressions(4).id, 1),
    )
    computeMetrics(impressions, clicks).appCountryRecommendations.sortBy(_.appID) shouldBe
      List(
        AppCountryRecommendation(0L, "CZ", List(42L)),
        AppCountryRecommendation(1L, "DE", List(47L)),
        AppCountryRecommendation(2L, "CZ", List(48L, 47L)),
      )
  }

  "recommend maximum 5 advertisers" in {
    val impressions = List(
      Impression(UUID.randomUUID(), 0L, "CZ", 42L),
      Impression(UUID.randomUUID(), 0L, "CZ", 43L),
      Impression(UUID.randomUUID(), 0L, "CZ", 44L),
      Impression(UUID.randomUUID(), 0L, "CZ", 45L),
      Impression(UUID.randomUUID(), 0L, "CZ", 46L),
      Impression(UUID.randomUUID(), 0L, "CZ", 47L),
    )
    val clicks = impressions.map(i => Click(i.id, 1))
    computeMetrics(impressions, clicks).appCountryRecommendations.head.recommendedAdvertisersIds.length shouldBe 5
  }

  def computeMetrics(impressions: List[Impression], clicks: List[Click]): ImpressionMetrics = {
    val list = Stream.resource(Blocker[IO]).flatMap { implicit blocker =>
      Metrics.compute[IO](Stream(impressions: _*), Stream(clicks: _*))
    }.compile.toList.unsafeRunSync()
    list.size shouldBe 1
    list.head
  }
}

package com.github.matobet.top_ads.model

import java.util.UUID

import cats._
import cats.implicits._
import cats.kernel.Monoid
import io.circe.generic.extras.ConfiguredJsonCodec
import io.scalaland.catnip._

@ConfiguredJsonCodec
case class Impression(
  id: ImpressionID,
  appId: AppID,
  countryCode: String,
  advertiserId: AdvertiserID
) {
  def toMetrics: ImpressionMetrics = ImpressionMetrics(
    Map(id -> ImpressionMetric(0, 0)),
    Map(AppCountryKey(appId, countryCode) -> Set(id))
  )
}

@ConfiguredJsonCodec
case class Click(
  impressionId: ImpressionID,
  revenue: Double
)

case class AppCountryKey(appId: AppID, countryCode: String)

@Semi(Monoid)
case class ImpressionMetric(clicks: Long, totalRevenue: Double) {
  def registerClick(click: Click): ImpressionMetric = ImpressionMetric(clicks + 1, totalRevenue + click.revenue)
}

@Semi(Monoid)
case class ImpressionMetrics(
  metricsById: Map[ImpressionID, ImpressionMetric],
  impressionsByCountryApp: Map[AppCountryKey, Set[ImpressionID]]
) {
  def registerClick(click: Click): ImpressionMetrics = copy(metricsById = metricsById.updatedWith(click.impressionId) { maybeMetric =>
    maybeMetric.map { _.registerClick(click) }
  })
}

@Semi(Monoid)
case class Metric(
  impressions: Set[UUID],
  clicks: Long,
  revenue: Double
)
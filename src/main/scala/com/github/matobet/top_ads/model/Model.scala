package com.github.matobet.top_ads.model

import cats._
import cats.implicits._
import cats.kernel.Monoid
import io.circe.generic.extras.ConfiguredJsonCodec
import io.scalaland.catnip._
import scala.collection.parallel.CollectionConverters._

@ConfiguredJsonCodec
case class Impression(
  id: ImpressionID,
  appId: AppID,
  countryCode: CountryCode,
  advertiserId: AdvertiserID
) {
  def toMetrics: ImpressionMetrics = ImpressionMetrics(
    Map(id -> ImpressionMetric(0, 0)),
    Map(id -> advertiserId),
    Map(AppCountryKey(appId, countryCode) -> Set(id))
  )
}

@ConfiguredJsonCodec
case class Click(
  impressionId: ImpressionID,
  revenue: Double
)

@ConfiguredJsonCodec
case class AppCountryTotals(
  appID: AppID,
  countryCode: CountryCode,
  impressions: Long,
  clicks: Long,
  revenue: Double
)

@ConfiguredJsonCodec
case class AppCountryRecommendation(
  appID: AppID,
  countryCode: CountryCode,
  recommendedAdvertisersIds: List[AdvertiserID]
)

case class AppCountryKey(appId: AppID, countryCode: CountryCode)

@Semi(Monoid)
case class ImpressionMetric(clicks: Long, totalRevenue: Double) {
  def registerClick(click: Click): ImpressionMetric = ImpressionMetric(clicks + 1, totalRevenue + click.revenue)
}

/**
 * The main monoidal structure that will be used to aggregate all data required for subsequent computations
 * from the streams of impression and clicks.
 */
@Semi(Monoid)
case class ImpressionMetrics(
  metricsById: Map[ImpressionID, ImpressionMetric],
  advertisersByImpressions: Map[ImpressionID, AdvertiserID],
  impressionsByCountryApp: Map[AppCountryKey, Set[ImpressionID]]
) {

  def registerClick(click: Click): ImpressionMetrics = copy(metricsById = metricsById.updatedWith(click.impressionId) {
    _.map { _.registerClick(click) }
  })

  def appCountryTotals: List[AppCountryTotals] = impressionsByCountryApp.par.map {
    case (AppCountryKey(appId, countryCode), impressionIds) =>
      val impressionMetrics = impressionIds.toSeq.map(metricsById)
      val totalClicks = impressionMetrics.map(_.clicks).sum
      val totalRevenue = impressionMetrics.map(_.totalRevenue).sum
      AppCountryTotals(appId, countryCode, impressionIds.size, totalClicks, totalRevenue)
  }.toList

  def appCountryRecommendations: List[AppCountryRecommendation] = impressionsByCountryApp.par.map {
    case (AppCountryKey(appId, countryCode), impressionIds) =>
      val topAdvertiserIds = impressionIds
        .groupBy(advertisersByImpressions)
        .view.mapValues(impressionIds => impressionIds.toList.foldMap(metricsById).totalRevenue / impressionIds.size)
        .toList
        .sortBy { case (_, revenuePerImpression) => revenuePerImpression } (Ordering[Double].reverse)
        .take(5)
        .map { case (advertiserId, _) => advertiserId }
      AppCountryRecommendation(appId, countryCode, topAdvertiserIds)
  }.toList
}

package com.github.matobet.top_ads

import java.util.UUID

import io.circe.{Encoder, Json}
import io.circe.generic.extras.Configuration

package object model {

  type ImpressionID = UUID
  type AppID = Long
  type AdvertiserID = Long

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val encodeImpressionMetrics: Encoder[ImpressionMetrics] = (metrics: ImpressionMetrics) =>
    Json.arr(metrics.impressionsByCountryApp.map {
      case (AppCountryKey(appId, countryCode), impressionIds) =>
        val impressions = impressionIds.toList.map(metrics.metricsById)
        Json.obj(
          "app_id" -> Json.fromString(appId.toString),
          "country_code" -> Json.fromString(countryCode),
          "impressions" -> Json.fromLong(impressionIds.size),
          "clicks" -> Json.fromLong(impressions.map(_.clicks).sum),
          "revenue" -> Json.fromDouble(impressions.map(_.totalRevenue).sum).get
        )
    }.toSeq: _*)
}

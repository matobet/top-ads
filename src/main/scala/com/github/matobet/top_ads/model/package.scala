package com.github.matobet.top_ads

import java.util.UUID

import io.circe.generic.extras.Configuration

package object model {

  type ImpressionID = UUID
  type AppID = Long
  type AdvertiserID = Long
  type CountryCode = String

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}

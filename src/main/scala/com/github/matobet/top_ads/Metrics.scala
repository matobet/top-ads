package com.github.matobet.top_ads

import cats.effect.Sync
import com.github.matobet.top_ads.model.{Click, Impression, ImpressionMetrics}
import fs2.Stream

object Metrics {
  def compute[F[_] : Sync](impressions: Stream[F, Impression], clicks: Stream[F, Click]): Stream[F, ImpressionMetrics] = {
    impressions.map(_.toMetrics).foldMonoid.flatMap { impressionMetrics =>
      clicks.fold(impressionMetrics) { _.registerClick(_) }
    }
  }
}

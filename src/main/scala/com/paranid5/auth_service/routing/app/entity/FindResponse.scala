package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class FindResponse(
  appId:        Long,
  appSecret:    String,
  appName:      String,
  appThumbnail: Option[String],
  callbackUrl:  Option[String],
  clientId:     Long,
)

object FindResponse:
  given Encoder[FindResponse] =
    Encoder.forProduct6(
      "app_id",
      "app_secret",
      "app_name",
      "app_thumbnail",
      "callback_url",
      "client_id"
    ): e ⇒
      (e.appId, e.appSecret, e.appName, e.appThumbnail, e.callbackUrl, e.clientId)

  given Decoder[FindResponse] =
    Decoder.forProduct6(
      "app_id",
      "app_secret",
      "app_name",
      "app_thumbnail",
      "callback_url",
      "client_id"
    )(FindResponse.apply)

package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class UpdateRequest(
  appId:        Long,
  appSecret:    String,
  appName:      String,
  appThumbnail: Option[String],
  callbackUrl:  Option[String],
)

object UpdateRequest:
  given Encoder[UpdateRequest] =
    Encoder.forProduct5(
      "app_id",
      "app_secret",
      "app_name",
      "app_thumbnail",
      "callback_url",
    ): e ⇒
      (e.appId, e.appSecret, e.appName, e.appThumbnail, e.callbackUrl)

  given Decoder[UpdateRequest] =
    Decoder.forProduct5(
      "app_id",
      "app_secret",
      "app_name",
      "app_thumbnail",
      "callback_url"
    )(UpdateRequest.apply)

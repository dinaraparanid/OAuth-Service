package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class CreateRequest(
  clientId:     Long,
  appName:      String,
  appThumbnail: Option[String],
  callbackUrl:  Option[String],
)

object CreateRequest:
  given Encoder[CreateRequest] =
    Encoder.forProduct4("client_id", "app_name", "app_thumbnail", "callback_url"): e ⇒
      (e.clientId, e.appName, e.appThumbnail, e.callbackUrl)

  given Decoder[CreateRequest] =
    Decoder.forProduct4("client_id", "app_name", "app_thumbnail", "callback_url")(CreateRequest.apply)

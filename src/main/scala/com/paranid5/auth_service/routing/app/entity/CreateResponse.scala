package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class CreateResponse(
  appId:     Long,
  appSecret: String,
)

object CreateResponse:
  given Encoder[CreateResponse] =
    Encoder.forProduct2("app_id", "app_secret"): e ⇒
      (e.appId, e.appSecret)

  given Decoder[CreateResponse] =
    Decoder.forProduct2("app_id", "app_secret")(CreateResponse.apply)

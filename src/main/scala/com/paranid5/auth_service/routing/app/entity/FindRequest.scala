package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class FindRequest(
  appId:     Long,
  appSecret: String,
)

object FindRequest:
  given Encoder[FindRequest] =
    Encoder.forProduct2("app_id", "app_secret"): e ⇒
      (e.appId, e.appSecret)

  given Decoder[FindRequest] =
    Decoder.forProduct2("app_id", "app_secret")(FindRequest.apply)
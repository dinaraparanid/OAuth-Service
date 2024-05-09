package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class DeleteRequest(
  clientId:  Long,
  appId:     Long,
  appSecret: String,
)

object DeleteRequest:
  given Encoder[DeleteRequest] =
    Encoder.forProduct3("client_id", "app_id", "app_secret"): e ⇒
      (e.clientId, e.appId, e.appSecret)

  given Decoder[DeleteRequest] =
    Decoder.forProduct3("client_id", "app_id", "app_secret")(DeleteRequest.apply)

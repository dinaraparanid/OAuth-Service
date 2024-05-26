package com.paranid5.auth_service.routing.auth.entity

import io.circe.{Decoder, Encoder}

final case class ConfirmEmailResponse(
  clientId:     Long,
  clientSecret: String,
)

object ConfirmEmailResponse:
  given Encoder[ConfirmEmailResponse] =
    Encoder.forProduct2("client_id", "client_secret"): e ⇒
      (e.clientId, e.clientSecret)

  given Decoder[ConfirmEmailResponse] =
    Decoder.forProduct2("client_id", "client_secret")(ConfirmEmailResponse.apply)
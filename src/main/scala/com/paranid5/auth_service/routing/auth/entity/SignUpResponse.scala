package com.paranid5.auth_service.routing.auth.entity

import io.circe.{Decoder, Encoder}

final case class SignUpResponse(
  clientId:     Long,
  clientSecret: String
)

object SignUpResponse:
  given Encoder[SignUpResponse] =
    Encoder.forProduct2("client_id", "client_secret"): e ⇒
      (e.clientId, e.clientSecret)

  given Decoder[SignUpResponse] =
    Decoder.forProduct2("client_id", "client_secret")(SignUpResponse.apply)
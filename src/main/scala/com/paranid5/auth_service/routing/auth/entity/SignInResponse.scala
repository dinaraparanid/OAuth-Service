package com.paranid5.auth_service.routing.auth.entity

import io.circe.{Encoder, Decoder}

final case class SignInResponse(
  clientId:     Long,
  clientSecret: String
)

object SignInResponse:
  given Encoder[SignInResponse] =
    Encoder.forProduct2("client_id", "client_secret"): e ⇒
      (e.clientId, e.clientSecret)

  given Decoder[SignInResponse] =
    Decoder.forProduct2("client_id", "client_secret")(SignInResponse.apply)

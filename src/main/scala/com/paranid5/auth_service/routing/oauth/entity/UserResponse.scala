package com.paranid5.auth_service.routing.oauth.entity

import io.circe.{Decoder, Encoder}

final case class UserResponse(
  clientId:     Long,
  clientSecret: String,
  username:     String,
  email:        String
)

object UserResponse:
  given Encoder[UserResponse] =
    Encoder.forProduct4("client_id", "client_secret", "username", "email"): e ⇒
      (e.clientId, e.clientSecret, e.username, e.email)

  given Decoder[UserResponse] =
    Decoder.forProduct4("client_id", "client_secret", "username", "email")(UserResponse.apply)

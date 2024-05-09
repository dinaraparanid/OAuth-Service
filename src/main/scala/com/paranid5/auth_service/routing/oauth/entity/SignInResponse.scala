package com.paranid5.auth_service.routing.oauth.entity

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import io.circe.{Decoder, Encoder}

final case class SignInResponse(
  clientId:     Long,
  clientSecret: String,
  accessToken:  TokenEntity,
  refreshToken: TokenEntity,
  redirectUrl:  String
)

object SignInResponse:
  given Encoder[SignInResponse] =
    Encoder.forProduct5("client_id", "client_secret", "access_token", "refresh_token", "redirect"): e ⇒
      (e.clientId, e.clientSecret, e.accessToken, e.refreshToken, e.redirectUrl)

  given Decoder[SignInResponse] =
    Decoder.forProduct5("client_id", "client_secret", "access_token", "refresh_token", "redirect")(SignInResponse.apply)

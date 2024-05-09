package com.paranid5.auth_service.routing.oauth.entity

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import io.circe.{Decoder, Encoder}

final case class TokenResponse(
  accessToken:  TokenEntity,
  refreshToken: TokenEntity,
  redirectUrl:  String
)

object TokenResponse:
  given Encoder[TokenResponse] =
    Encoder.forProduct3("access_token", "refresh_token", "redirect"): e ⇒
      (e.accessToken, e.refreshToken, e.redirectUrl)

  given Decoder[TokenResponse] =
    Decoder.forProduct3("access_token", "refresh_token", "redirect")(TokenResponse.apply)

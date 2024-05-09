package com.paranid5.auth_service.routing.oauth.entity

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import io.circe.{Decoder, Encoder}

final case class RefreshResponse(accessToken: TokenEntity)

object RefreshResponse:
  given Encoder[RefreshResponse] =
    Encoder.forProduct1("access_token"): e ⇒
      e.accessToken

  given Decoder[RefreshResponse] =
    Decoder.forProduct1("access_token")(RefreshResponse.apply)

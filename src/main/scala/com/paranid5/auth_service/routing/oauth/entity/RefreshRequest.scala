package com.paranid5.auth_service.routing.oauth.entity

import io.circe.{Decoder, Encoder}

final case class RefreshRequest(token: String)

object RefreshRequest:
  given Encoder[RefreshRequest] =
    Encoder.forProduct1("token"): e ⇒
      e.token

  given Decoder[RefreshRequest] =
    Decoder.forProduct1("token")(RefreshRequest.apply)

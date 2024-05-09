package com.paranid5.auth_service.routing.oauth.entity

import io.circe.{Decoder, Encoder}

final case class AuthorizeRequest(token: String)

object AuthorizeRequest:
  given Encoder[AuthorizeRequest] =
    Encoder.forProduct1("token"): e ⇒
      e.token

  given Decoder[AuthorizeRequest] =
    Decoder.forProduct1("token")(AuthorizeRequest.apply)

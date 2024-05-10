package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class AllResponse(apps: List[AppResponse])

object AllResponse:
  given Encoder[AllResponse] =
    Encoder.forProduct1("apps"): e ⇒
      e.apps

  given Decoder[AllResponse] =
    Decoder.forProduct1("apps")(AllResponse.apply)
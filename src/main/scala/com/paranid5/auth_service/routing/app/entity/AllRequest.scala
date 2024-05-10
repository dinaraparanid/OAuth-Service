package com.paranid5.auth_service.routing.app.entity

import io.circe.{Decoder, Encoder}

final case class AllRequest(clientId: Long)

object AllRequest:
  given Encoder[AllRequest] =
    Encoder.forProduct1("client_id"): e ⇒
      (e.clientId)

  given Decoder[AllRequest] =
    Decoder.forProduct1("client_id")(AllRequest.apply)
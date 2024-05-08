package com.paranid5.auth_service.routing.auth.entity

import io.circe.{Decoder, Encoder}

final case class LoginPasswordEntity(
  login:    String,
  password: String
)

object LoginPasswordEntity:
  given Encoder[LoginPasswordEntity] =
    Encoder.forProduct2("login", "password"): e ⇒
      (e.login, e.password)

  given Decoder[LoginPasswordEntity] =
    Decoder.forProduct2("login", "password")(LoginPasswordEntity.apply)

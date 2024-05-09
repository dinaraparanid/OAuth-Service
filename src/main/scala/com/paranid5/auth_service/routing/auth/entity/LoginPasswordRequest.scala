package com.paranid5.auth_service.routing.auth.entity

import com.paranid5.auth_service.domain.encodedToSha

import io.circe.{Decoder, Encoder}

final case class LoginPasswordRequest(
  login:    String,
  password: String
)

object LoginPasswordRequest:
  given Encoder[LoginPasswordRequest] =
    Encoder.forProduct2("login", "password"): e ⇒
      (e.login, e.password)

  given Decoder[LoginPasswordRequest] =
    Decoder.forProduct2("login", "password")(LoginPasswordRequest.apply)

  given EncodePassword[LoginPasswordRequest] with
    extension (self: LoginPasswordRequest)
      override def withEncodedPassword: LoginPasswordRequest =
        LoginPasswordRequest(
          login    = self.login,
          password = self.password.encodedToSha getOrElse ""
        )

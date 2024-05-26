package com.paranid5.auth_service.routing.auth.entity

import com.paranid5.auth_service.domain.encodedToSha

import io.circe.{Decoder, Encoder}

final case class SignUpRequest(
  username:  String,
  email:     String,
  password:  String,
  confirmUrl: String,
)

object SignUpRequest:
  given Encoder[SignUpRequest] =
    Encoder.forProduct4("username", "email", "password", "confirm_url"): e ⇒
      (e.username, e.email, e.password, e.confirmUrl)

  given Decoder[SignUpRequest] =
    Decoder.forProduct4("username", "email", "password", "confirm_url")(SignUpRequest.apply)

  given EncodePassword[SignUpRequest] with
    extension (self: SignUpRequest)
      override def withEncodedPassword: SignUpRequest =
        SignUpRequest(
          username  = self.username,
          email     = self.email,
          password  = self.password.encodedToSha getOrElse "",
          confirmUrl = self.confirmUrl
        )
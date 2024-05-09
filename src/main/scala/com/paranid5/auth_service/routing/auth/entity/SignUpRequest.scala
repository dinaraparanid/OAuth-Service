package com.paranid5.auth_service.routing.auth.entity

import com.paranid5.auth_service.domain.encodedToSha

import io.circe.{Decoder, Encoder}

final case class SignUpRequest(
  username: String,
  email:    String,
  password: String
)

object SignUpRequest:
  given Encoder[SignUpRequest] =
    Encoder.forProduct3("username", "email", "encoded_password"): e ⇒
      (e.username, e.email, e.password)

  given Decoder[SignUpRequest] =
    Decoder.forProduct3("username", "email", "encoded_password")(SignUpRequest.apply)

  given EncodePassword[SignUpRequest] with
    extension (self: SignUpRequest)
      override def withEncodedPassword: SignUpRequest =
        SignUpRequest(
          username = self.username,
          email    = self.email,
          password = self.password.encodedToSha getOrElse ""
        )
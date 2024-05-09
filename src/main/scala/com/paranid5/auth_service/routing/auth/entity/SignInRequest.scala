package com.paranid5.auth_service.routing.auth.entity

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.domain.encodedToSha
import io.circe.{Decoder, Encoder}

final case class SignInRequest(
  email:    String,
  password: String
)

object SignInRequest:
  given Encoder[SignInRequest] =
    Encoder.forProduct2("email", "password"): e ⇒
      (e.email, e.password)

  given Decoder[SignInRequest] =
    Decoder.forProduct2("email", "password")(SignInRequest.apply)

  given EncodePassword[SignInRequest] with
    extension (self: SignInRequest)
      override def withEncodedPassword: SignInRequest =
        SignInRequest(
          email    = self.email,
          password = self.password.encodedToSha getOrElse ""
        )

extension (request: SignInRequest)
  infix def matches(user: User): Boolean =
    request.email == user.email && request.password == user.encodedPassword
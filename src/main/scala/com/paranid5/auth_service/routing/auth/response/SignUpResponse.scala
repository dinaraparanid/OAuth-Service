package com.paranid5.auth_service.routing.auth.response

import cats.effect.IO

import org.http4s.Response
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*

private[auth] def confirmationEmailSuccessfullySent: IO[Response[IO]] =
  Ok("Confirmation email was successfully sent")

private[auth] def confirmationCodeGenerationError: IO[Response[IO]] =
  InternalServerError("Confirmation code generation error")

private[auth] def confirmationEmailNotSent: IO[Response[IO]] =
  InternalServerError("Confirmation email was not sent")

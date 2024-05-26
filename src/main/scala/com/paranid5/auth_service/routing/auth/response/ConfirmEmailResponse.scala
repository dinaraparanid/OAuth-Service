package com.paranid5.auth_service.routing.auth.response

import cats.effect.IO

import com.paranid5.auth_service.routing.auth.entity.ConfirmEmailResponse

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.Response
import org.http4s.dsl.io.*

private[auth] def invalidConfirmCode: IO[Response[IO]] =
  NotFound("Invalid confirm code")

private[auth] def userNotFound: IO[Response[IO]] =
  NotFound("User was not found")

private[auth] def emailIsConfirmed(
  clientId:     Long,
  clientSecret: String,
): IO[Response[IO]] =
  Created(ConfirmEmailResponse(clientId, clientSecret).asJson)

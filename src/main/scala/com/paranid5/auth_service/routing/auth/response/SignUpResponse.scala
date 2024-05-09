package com.paranid5.auth_service.routing.auth.response

import cats.effect.IO

import com.paranid5.auth_service.routing.auth.entity.SignUpResponse

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.Response
import org.http4s.dsl.io.*

def userSuccessfullyRegistered(
  clientId:     Long,
  clientSecret: String,
): IO[Response[IO]] =
  Created(SignUpResponse(clientId, clientSecret).asJson)

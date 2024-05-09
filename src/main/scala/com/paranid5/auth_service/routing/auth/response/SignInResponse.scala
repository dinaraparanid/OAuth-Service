package com.paranid5.auth_service.routing.auth.response

import cats.effect.IO

import com.paranid5.auth_service.routing.auth.entity.SignInResponse

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.Response

def userSignedIn(
  clientId:     Long,
  clientSecret: String,
): IO[Response[IO]] =
  Ok(SignInResponse(clientId, clientSecret).asJson)

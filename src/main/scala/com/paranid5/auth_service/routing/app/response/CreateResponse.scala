package com.paranid5.auth_service.routing.app.response

import cats.effect.IO

import com.paranid5.auth_service.routing.app.entity.CreateResponse

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.Response

def appSuccessfullyCreated(
  appId:     Long,
  appSecret: String,
): IO[Response[IO]] =
  Created(CreateResponse(appId, appSecret).asJson)

def appNameMustNotBeEmpty: IO[Response[IO]] =
  BadRequest("App name must not be empty")

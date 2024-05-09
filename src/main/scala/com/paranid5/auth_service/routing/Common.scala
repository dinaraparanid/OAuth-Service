package com.paranid5.auth_service.routing

import cats.effect.IO
import org.http4s.Response
import org.http4s.dsl.io.*

private val DefaultRedirect = "http://0.0.0.0:4000/"

private def invalidBody: IO[Response[IO]] =
  BadRequest("Invalid body")

private def redirectToCallbackUrl(callbackUrl: String): IO[Response[IO]] =
  Found(callbackUrl)

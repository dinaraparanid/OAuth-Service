package com.paranid5.auth_service.routing.app.response

import cats.effect.IO
import org.http4s.Response
import org.http4s.dsl.io.*

def appSuccessfullyUpdated: IO[Response[IO]] =
  Ok("App successfully updated")

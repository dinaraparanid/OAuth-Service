package com.paranid5.auth_service.routing.app.response

import cats.effect.IO
import org.http4s.dsl.io.*
import org.http4s.Response

def appSuccessfullyDeleted: IO[Response[IO]] =
  Ok("App successfully deleted")

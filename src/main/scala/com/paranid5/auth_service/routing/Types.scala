package com.paranid5.auth_service.routing

import cats.effect.IO
import com.paranid5.auth_service.di.AppDependencies
import org.http4s.{HttpRoutes, Request, Response}

private type AppRoutes       = AppDependencies[HttpRoutes[IO]]
private type AppHttpResponse = AppDependencies[IO[Response[IO]]]

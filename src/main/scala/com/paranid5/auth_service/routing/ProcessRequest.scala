package com.paranid5.auth_service.routing

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*

import org.http4s.{DecodeResult, Response}

def processRequest[R](requestRes: DecodeResult[IO, R])(
  handleRequest: R ⇒ ConnectionIO[IO[Response[IO]]]
): AppHttpResponse =
  Reader: appModule ⇒
    requestRes
      .fold(fa = Left(_), fb = handleRequest >>> (Right(_)))
      .map: res ⇒
        res
          .sequence
          .map(_ getOrElse invalidBody)
          .transact(appModule.transcactor)
      .flatten
      .flatten

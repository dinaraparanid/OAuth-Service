package com.paranid5.auth_service.routing.app.response

import cats.effect.IO
import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.routing.app.entity.{AllResponse, AppResponse, FindResponse}
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.Response

def clientApps(apps: List[AppEntity]): IO[Response[IO]] =
  Ok:
    AllResponse:
      apps map: app ⇒
        AppResponse(
          appId        = app.appId,
          appSecret    = app.appSecret,
          appName      = app.appName,
          appThumbnail = app.appThumbnail,
          callbackUrl  = app.callbackUrl
        )
    .asJson

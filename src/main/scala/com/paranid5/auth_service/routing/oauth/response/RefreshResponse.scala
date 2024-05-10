package com.paranid5.auth_service.routing.oauth.response

import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.routing.oauth.entity.{RefreshResponse, TokenResponse}

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.Response
import org.http4s.dsl.io.*

def accessTokenRefreshed(accessToken: TokenEntity): IO[Response[IO]] =
  Created(RefreshResponse(accessToken).asJson)
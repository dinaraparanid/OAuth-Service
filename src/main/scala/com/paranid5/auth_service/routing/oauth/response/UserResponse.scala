package com.paranid5.auth_service.routing.oauth.response

import cats.effect.IO
import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.oauth.entity.UserResponse
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.Response
import org.http4s.dsl.io.*

def userMetadata(
  user:   User,
  client: ClientEntity
): IO[Response[IO]] =
  Ok:
    UserResponse(
      clientId     = client.clientId,
      clientSecret = client.clientSecret,
      username     = user.username,
      email        = user.email
    )
      

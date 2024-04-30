package com.paranid5.auth_service.data.oauth

import cats.Applicative
import com.paranid5.auth_service.data.oauth.token.AccessTokenCreationFailure
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken}
import io.github.cdimascio.dotenv.Dotenv

trait OAuthRepository[F[_] : Applicative, R]:
  def connect(dotenv: Dotenv): R

  extension (repository: R)
    def getClientWithTokens(clientId: Long): F[Client]

    def isAccessTokenValid(
      clientId:     Long,
      clientSecret: String,
      accessToken:  String
    ): F[Boolean]

    def isRefreshTokenValid(
      clientId:     Long,
      clientSecret: String,
      refreshToken: String
    ): F[Boolean]

    def isClientExits(
      clientId:     Long,
      clientSecret: String
    ): F[Boolean]

    def storeClient(
      clientId:     Long,
      clientSecret: String
    ): F[Unit]

    def deleteClient(clientId: Long): F[Unit]

    def userAccessTokens(userId: Long): F[List[AccessToken]]

    def newAccessToken(refreshToken: RefreshToken): F[Either[AccessTokenCreationFailure, AccessToken]]

    def newRefreshToken(clientId: Long, clientSecret: String): F[Option[RefreshToken]]




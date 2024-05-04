package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenScope}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason

import cats.Applicative

import io.github.cdimascio.dotenv.Dotenv

trait OAuthRepository[F[_] : Applicative, R]:
  type TokenAttemptF[T] = F[Either[InvalidTokenReason, T]]

  def connect(dotenv: Dotenv): R

  extension (repository: R)
    def getClientWithTokens(
      clientId:     Long,
      clientSecret: String
    ): F[Option[Client]]

    def isTokenValid(
      clientId:     Long,
      clientSecret: String,
      tokenValue:   String
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

    def getClientAccessTokens(clientId: Long): F[List[AccessToken]]

    def newAccessToken(
      refreshToken:     RefreshToken,
      accessTokenTitle: String,
      lifeSeconds:      Option[Long],
      scopes:           List[TokenScope]
    ): TokenAttemptF[AccessToken]

    def newRefreshToken(
      clientId:     Long,
      clientSecret: String
    ): TokenAttemptF[RefreshToken]

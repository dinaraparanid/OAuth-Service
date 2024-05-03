package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.oauth.token.InvalidTokenReason
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken}


import cats.Applicative

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

    def newAccessToken(refreshToken: RefreshToken): F[Either[InvalidTokenReason, AccessToken]]

    def newRefreshToken(clientId: Long, clientSecret: String): F[Option[RefreshToken]]




package com.paranid5.auth_service.data.oauth.token

import cats.Applicative

import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason

trait TokenDataSource[F[_] : Applicative, S]:
  type TokenAttemptF[T] = F[Either[InvalidTokenReason, T]]

  extension (source: S)
    def createTable(): F[Unit]

    def getClientAccessTokens(clientId: Long): F[List[TokenEntity]]

    def getPlatformClientAccessToken(clientId: Long): F[Option[TokenEntity]]

    def getClientRefreshToken(clientId: Long): F[Option[TokenEntity]]

    def findToken(
      clientId:     Long,
      tokenValue:   String
    ): TokenAttemptF[TokenEntity]

    def retrieveToken(tokenValue: String): TokenAttemptF[TokenEntity]

    def getTokenByAppId(
      clientId:   Long,
      appId: Long
    ): F[Option[TokenEntity]]

    def newAppAccessToken(
      refreshToken: RefreshToken,
      appId:        Long,
      lifeSeconds:  Option[Long],
      tokenValue:   String,
    ): TokenAttemptF[TokenEntity]

    def newPlatformAccessToken(
      refreshToken: RefreshToken,
      lifeSeconds:  Option[Long],
      tokenValue:   String,
    ): TokenAttemptF[TokenEntity]

    def newRefreshToken(
      clientId:     Long,
      clientSecret: String,
      tokenValue:   String,
    ): TokenAttemptF[RefreshToken]

    def isTokenValid(token: TokenEntity): TokenAttemptF[Unit]

    def getToken(
      clientId: Long,
      appId:    Option[Long],
      value:    String
    ): F[Option[TokenEntity]]

    def deleteToken(
      clientId: Long,
      appId:    Option[Long],
      status:   String,
    ): TokenAttemptF[Unit]

package com.paranid5.auth_service.data.oauth.token

import cats.Applicative

import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason

trait TokenDataSource[F[_] : Applicative, S]:
  type TokenAttemptF[T] = F[Either[InvalidTokenReason, T]]

  extension (source: S)
    infix def getClientAccessTokens(clientId: Long): F[List[TokenEntity]]

    infix def getClientRefreshToken(clientId: Long): F[Option[TokenEntity]]

    def getToken(
      clientId:     Long,
      tokenValue:   String
    ): F[Option[TokenEntity]]

    def getTokenByTitle(
      clientId:   Long,
      tokenTitle: String
    ): F[Option[TokenEntity]]

    def newAccessToken(
      refreshToken: RefreshToken,
      title:        String,
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
      title:    Option[String],
      value:    String
    ): F[Option[TokenEntity]]

    def deleteToken(
      clientId: Long,
      title:    Option[String]
    ): TokenAttemptF[Unit]

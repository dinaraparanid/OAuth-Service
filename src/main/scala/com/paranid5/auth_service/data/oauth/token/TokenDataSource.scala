package com.paranid5.auth_service.data.oauth.token

import cats.Applicative
import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity}

trait TokenDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def userAccessTokens(userId: Long): F[List[TokenEntity]]

    def newAccessToken(
      refreshToken: RefreshToken,
      title:        String,
      lifeSeconds:  Option[Long],
      tokenValue:   String,
    ): F[Either[InvalidTokenReason, TokenEntity]]

    def newRefreshToken(
      clientId:     Long,
      clientSecret: String,
      tokenValue:   String,
    ): F[Either[InvalidTokenReason, RefreshToken]]

    def isTokenValid(token: TokenEntity): F[Either[InvalidTokenReason, Unit]]

    def getToken(
      userId: Long,
      title:  Option[String],
      value:  String
    ): F[Option[TokenEntity]]
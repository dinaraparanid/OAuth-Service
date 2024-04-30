package com.paranid5.auth_service.data.oauth.token

import cats.Applicative
import com.paranid5.auth_service.data.oauth.token.AccessTokenCreationFailure
import com.paranid5.auth_service.data.oauth.token.entity.AccessToken
import scalaoauth2.provider.RefreshToken

trait TokenDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def userAccessTokens(userId: Long): F[List[AccessToken]]

    def newAccessToken(refreshToken: RefreshToken): F[Either[AccessTokenCreationFailure, AccessToken]]

    def newRefreshToken(clientId: Long, clientSecret: String): F[Option[RefreshToken]]
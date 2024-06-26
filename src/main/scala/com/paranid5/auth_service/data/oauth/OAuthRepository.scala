package com.paranid5.auth_service.data.oauth

import cats.Applicative
import cats.data.ValidatedNec
import com.paranid5.auth_service.data.oauth.client.entity.{AppEntity, ClientEntity}
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenEntity, TokenScope}
import com.paranid5.auth_service.data.oauth.token.error.*

private val AccessTokenAliveTime: Long = 1000 * 60 * 5 // 5 minutes

trait OAuthRepository[F[_] : Applicative, R]:
  type OAuthAttemptF  [T] = F[Either[InvalidOAuthReason, T]]
  type OAuthValidatedF[T] = F[ValidatedNec[InvalidOAuthReason, T]]
  type TokenAttemptF  [T] = F[Either[InvalidTokenReason, T]]
  type TokenValidatedF[T] = F[ValidatedNec[InvalidTokenReason, T]]

  extension (repository: R)
    def createTables(): F[Unit]

    def getClient(clientId: Long): F[Option[ClientEntity]]

    def findClient(
      clientId:     Long,
      clientSecret: String
    ): F[Option[ClientEntity]]

    def getClientWithTokens(clientId: Long): F[Option[Client]]

    def isTokenValid(
      clientId:     Long,
      tokenValue:   String
    ): F[Either[InvalidTokenReason, Unit]]

    def isClientExits(
      clientId:     Long,
      clientSecret: String
    ): F[Boolean]

    def insertClient(
      clientId:     Long,
      clientSecret: String
    ): F[Unit]

    def deleteClient(clientId: Long): F[Unit]

    def getClientApps(clientId: Long): F[List[AppEntity]]

    def getApp(
      appId:     Long,
      appSecret: String
    ): F[Option[AppEntity]]

    def insertApp(
      appSecret:    String,
      appName:      String,
      appThumbnail: Option[String],
      callbackUrl:  Option[String],
      clientId:     Long,
    ): F[Long]

    def deleteApp(
      clientId:  Long,
      appId:     Long,
      appSecret: String
    ): F[Unit]

    def updateApp(
      appId:           Long,
      newAppName:      String,
      newAppThumbnail: Option[String],
      newCallbackUrl:  Option[String],
    ): F[Unit]

    def getPlatformClientAccessToken(clientId: Long): F[Option[AccessToken]]

    def getAppAccessToken(
      clientId:  Long,
      appId:     Long,
      appSecret: String
    ): F[Option[AccessToken]]

    def findToken(
      clientId:   Long,
      tokenValue: String
    ): TokenAttemptF[TokenEntity]

    def retrieveToken(tokenValue: String): TokenAttemptF[TokenEntity]

    def newAppAccessToken(
      refreshToken:     RefreshToken,
      accessTokenAppId: Long,
      lifeSeconds:      Option[Long]     = Option(AccessTokenAliveTime),
      scopes:           List[TokenScope] = Nil
    ): TokenAttemptF[AccessToken]

    def newPlatformAccessToken(
      refreshToken:     RefreshToken,
      lifeSeconds:      Option[Long]     = Option(AccessTokenAliveTime),
      scopes:           List[TokenScope] = Nil
    ): TokenAttemptF[AccessToken]

    def newRefreshToken(
      clientId:     Long,
      clientSecret: String,
    ): TokenAttemptF[RefreshToken]

    def deleteAccessTokenWithScopes(
      clientId: Long,
      appId:    Option[Long],
    ): OAuthValidatedF[Unit]

    def deletePlatformAccessTokenWithScopes(clientId: Long): OAuthValidatedF[Unit] =
      repository.deleteAccessTokenWithScopes(clientId = clientId, appId = None)

    def deleteRefreshToken(clientId: Long): TokenAttemptF[Unit]

    def deleteClientTokensWithScopes(clientId: Long): OAuthValidatedF[Unit]

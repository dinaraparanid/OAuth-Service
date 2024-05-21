package com.paranid5.auth_service.data.oauth

import cats.data.ValidatedNec
import cats.syntax.all.*

import com.paranid5.auth_service.data.*
import com.paranid5.auth_service.data.oauth.client.entity.{AppEntity, ClientEntity}
import com.paranid5.auth_service.data.oauth.client.{PostgresAppDataSource, PostgresClientDataSource}
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenEntity, TokenScope, TokenStatus}
import com.paranid5.auth_service.data.oauth.token.error.*
import com.paranid5.auth_service.data.oauth.token.{PostgresTokenDataSource, PostgresTokenScopeDataSource}
import com.paranid5.auth_service.domain.generateToken

import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*

final class PostgresOAuthRepository(
  private val clientDataSource:     PostgresClientDataSource,
  private val appDataSource:        PostgresAppDataSource,
  private val tokenDataSource:      PostgresTokenDataSource,
  private val tokenScopeDataSource: PostgresTokenScopeDataSource
)

object PostgresOAuthRepository:
  private type TokenScopeAttemptCIO[T] = ConnectionIO[Either[InvalidScopeReason, T]]

  given OAuthRepository[ConnectionIO, PostgresOAuthRepository] with
    override type OAuthAttemptF  [T] = ConnectionIO[Either[InvalidOAuthReason, T]]
    override type OAuthValidatedF[T] = ConnectionIO[ValidatedNec[InvalidOAuthReason, T]]
    override type TokenAttemptF  [T] = ConnectionIO[Either[InvalidTokenReason, T]]
    override type TokenValidatedF[T] = ConnectionIO[ValidatedNec[InvalidTokenReason, T]]

    extension (repository: PostgresOAuthRepository)
      override def createTables(): ConnectionIO[Unit] =
        for
          _ ← repository.clientDataSource.createTable()
          _ ← repository.appDataSource.createTable()
          _ ← repository.tokenDataSource.createTable()
          _ ← repository.tokenScopeDataSource.createTable()
        yield ()

      override def getClient(clientId: Long): ConnectionIO[Option[ClientEntity]] =
        repository
          .clientDataSource
          .getClient(clientId)

      override def findClient(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Option[ClientEntity]] =
        repository
          .clientDataSource
          .findClient(clientId, clientSecret)

      override def getClientWithTokens(clientId: Long): ConnectionIO[Option[Client]] =
        for
          client       ← repository.clientDataSource.getClient(clientId)
          accessTokens ← repository.getClientAccessTokensCIO(clientId)
          refreshToken ← repository.tokenDataSource.getClientRefreshToken(clientId)
        yield client map (Client(_, accessTokens, refreshToken))

      override def isTokenValid(
        clientId:     Long,
        tokenValue:   String
      ): ConnectionIO[Either[InvalidTokenReason, Unit]] =
        for
          tokenRes ← repository.tokenDataSource.findToken(clientId, tokenValue)
          isValid  ← tokenRes.map(repository.tokenDataSource.isTokenValid).sequence
        yield isValid.flatten

      override def isClientExits(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Boolean] =
        repository.isClientExistsCIO(clientId, clientSecret)

      override def insertClient(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Unit] =
        repository
          .clientDataSource
          .insertClient(clientId, clientSecret)

      override def deleteClient(clientId: Long): ConnectionIO[Unit] =
        for
          _ ← repository.clientDataSource.deleteClient(clientId)
          _ ← repository.appDataSource.deleteClientApps(clientId)
          _ ← repository.deleteClientTokensWithScopesCIO(clientId)
        yield ()

      override def getClientApps(clientId: Long): ConnectionIO[List[AppEntity]] =
        repository
          .appDataSource
          .getClientApps(clientId)

      override def getApp(
        appId:     Long,
        appSecret: String
      ): ConnectionIO[Option[AppEntity]] =
        repository
          .appDataSource
          .getApp(appId, appSecret)

      override def insertApp(
        appSecret:    String,
        appName:      String,
        appThumbnail: Option[String],
        callbackUrl:  Option[String],
        clientId:     Long,
      ): ConnectionIO[Long] =
        repository
          .appDataSource
          .insertApp(
            appSecret    = appSecret,
            appName      = appName,
            appThumbnail = appThumbnail,
            callbackUrl  = callbackUrl,
            clientId     = clientId,
          )

      override def deleteApp(
        clientId:  Long,
        appId:     Long,
        appSecret: String
      ): ConnectionIO[Unit] =
        for
          _ ← repository.deleteAppAccessTokenWithScopesCIO(clientId, appId, appSecret)
          _ ← repository.appDataSource.deleteApp(appId)
        yield ()

      override def updateApp(
        appId:           Long,
        newAppName:      String,
        newAppThumbnail: Option[String],
        newCallbackUrl:  Option[String],
      ): ConnectionIO[Unit] =
        repository
          .appDataSource
          .updateApp(
            appId           = appId,
            newAppName      = newAppName,
            newAppThumbnail = newAppThumbnail,
            newCallbackUrl  = newCallbackUrl
          )

      override def getPlatformClientAccessToken(clientId: Long): ConnectionIO[Option[AccessToken]] =
        repository.getPlatformClientAccessTokenCIO(clientId)

      override def getAppAccessToken(
        clientId:  Long,
        appId:     Long,
        appSecret: String
      ): ConnectionIO[Option[AccessToken]] =
        repository.getAppAccessTokenCIO(clientId, appId, appSecret)

      override def findToken(
        clientId:   Long,
        tokenValue: String
      ): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
        repository
          .tokenDataSource
          .findToken(clientId, tokenValue)

      override def retrieveToken(
        tokenValue: String
      ): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
        repository
          .tokenDataSource
          .retrieveToken(tokenValue)

      override def newAppAccessToken(
        refreshToken:     RefreshToken,
        accessTokenAppId: Long,
        lifeSeconds:      Option[Long],
        scopes:           List[TokenScope]
      ): ConnectionIO[Either[InvalidTokenReason, AccessToken]] =
        def impl(
          tokenValue: String
        ): TokenAttemptF[AccessToken] =
          for
            token ← repository.tokenDataSource.newAppAccessToken(
              refreshToken = refreshToken,
              appId        = accessTokenAppId,
              lifeSeconds  = lifeSeconds,
              tokenValue   = tokenValue
            )

            _ ← repository.tokenScopeDataSource.addScopesToToken(
              clientId         = refreshToken.clientId,
              accessTokenAppId = Option(accessTokenAppId),
              scopes           = scopes
            )
          yield token map (t ⇒ AccessToken(entity = t, scopes = scopes))

        buildToken(tokenPrefix = f"$accessTokenAppId")(impl)

      override def newPlatformAccessToken(
        refreshToken:     RefreshToken,
        lifeSeconds:      Option[Long]     = Option(AccessTokenAliveTime),
        scopes:           List[TokenScope] = Nil
      ): TokenAttemptF[AccessToken] =
        def impl(
          tokenValue: String
        ): TokenAttemptF[AccessToken] =
          for
            token ← repository.tokenDataSource.newPlatformAccessToken(
              refreshToken = refreshToken,
              lifeSeconds  = lifeSeconds,
              tokenValue   = tokenValue
            )

            _ ← repository.tokenScopeDataSource.addScopesToToken(
              clientId         = refreshToken.clientId,
              accessTokenAppId = None,
              scopes           = scopes
            )
          yield token map (t ⇒ AccessToken(entity = t, scopes = scopes))

        buildToken(tokenPrefix = f"${refreshToken.createdAt}")(impl)

      override def newRefreshToken(
        clientId:     Long,
        clientSecret: String
      ): TokenAttemptF[RefreshToken] =
        def impl(tokenValue: String): TokenAttemptF[RefreshToken] =
          for token ← repository.tokenDataSource.newRefreshToken(
            clientId     = clientId,
            clientSecret = clientSecret,
            tokenValue   = tokenValue
          ) yield token

        buildToken(tokenPrefix = f"$clientId")(impl)

      override def deleteAccessTokenWithScopes(
        clientId: Long,
        appId:    Option[Long],
      ): ConnectionIO[ValidatedNec[InvalidOAuthReason, Unit]] =
        repository.deleteAccessTokenWithScopesCIO(clientId, appId)

      override def deleteRefreshToken(clientId: Long): ConnectionIO[Either[InvalidTokenReason, Unit]] =
        repository.deleteRefreshTokenCIO(clientId)

      override def deleteClientTokensWithScopes(
        clientId: Long
      ): ConnectionIO[ValidatedNec[InvalidOAuthReason, Unit]] =
        repository.deleteClientTokensWithScopesCIO(clientId)

      private def isClientExistsCIO(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Boolean] =
        for clientOpt ← repository.clientDataSource.findClient(clientId, clientSecret)
          yield clientOpt.isDefined

      private def getClientAccessTokensCIO(
        clientId: Long
      ): ConnectionIO[List[AccessToken]] =
        for
          accToksE     ← repository.tokenDataSource.getClientAccessTokens(clientId)
          accessTokens ← repository.tokenScopeDataSource.getAccessTokensWithScopes(accToksE)
        yield accessTokens

      private def getAppAccessTokenCIO(
        clientId:  Long,
        appId:     Long,
        appSecret: String,
      ): ConnectionIO[Option[AccessToken]] =
        def retrieveAppToken: ConnectionIO[Option[TokenEntity]] =
          repository
            .tokenDataSource
            .getTokenByAppId(clientId, appId)

        def retrieveTokenWithScope(
          appToken: Option[TokenEntity]
        ): ConnectionIO[Option[AccessToken]] =
          appToken
            .map: token ⇒
              repository
                .tokenScopeDataSource
                .getAccessTokensWithScopes(List(token))
                .map(_.headOption)
            .sequence
            .map(_.flatten)

        for
          token       ← retrieveAppToken
          accessToken ← retrieveTokenWithScope(token)
        yield accessToken

      private def getPlatformClientAccessTokenCIO(
        clientId: Long
      ): ConnectionIO[Option[AccessToken]] =
        for
          accTokOpt    ← repository.tokenDataSource.getPlatformClientAccessToken(clientId)
          accTokList   = accTokOpt map (List(_)) getOrElse Nil
          accessTokens ← repository.tokenScopeDataSource.getAccessTokensWithScopes(accTokList)
        yield accessTokens.headOption

      private def deleteAccessTokenWithScopesCIO(
        clientId: Long,
        appId:    Option[Long],
      ): ConnectionIO[ValidatedNec[InvalidOAuthReason, Unit]] =
        def removeScopes(): TokenScopeAttemptCIO[Unit] =
          repository
            .tokenScopeDataSource
            .removeAllScopesFromToken(
              clientId         = clientId,
              accessTokenAppId = appId
            )

        def removeToken(): TokenAttemptF[Unit] =
          repository.tokenDataSource.deleteToken(
            clientId = clientId,
            appId    = appId,
            status   = TokenStatus.Access.title
          )

        for
          rmScopesRes ← removeScopes()
          rmTokenRes  ← removeToken()

          rmScopesVal: ValidatedNec[InvalidOAuthReason, Unit] = rmScopesRes.toValidatedNec
          rmTokenVal:  ValidatedNec[InvalidOAuthReason, Unit] = rmTokenRes.toValidatedNec
        yield (rmScopesVal, rmTokenVal) mapN ((_, _) ⇒ ())

      private def deleteClientTokensWithScopesCIO(clientId: Long): OAuthValidatedF[Unit] =
        for
          accessTokens  ← repository.getClientAccessTokensCIO(clientId)
          accessTokVal  ← deleteAccessTokensWithScopesCIO(clientId, accessTokens)
          refreshTokRes ← deleteRefreshTokenCIO(clientId)
          refreshTokVal = refreshTokRes.toValidatedNec
        yield (refreshTokVal, accessTokVal) mapN ((_, _) ⇒ ())

      private def deleteAppAccessTokenWithScopesCIO(
        clientId:  Long,
        appId:     Long,
        appSecret: String,
      ): OAuthValidatedF[Unit] =
        for
          accessToken  ← repository.getAppAccessTokenCIO(clientId, appId, appSecret)
          accessTokens = accessToken map (List(_)) getOrElse Nil
          accessTokVal ← deleteAccessTokensWithScopesCIO(clientId, accessTokens)
        yield accessTokVal

      private def deleteAccessTokensWithScopesCIO(
        clientId: Long,
        tokens:   List[AccessToken]
      ): OAuthValidatedF[Unit] =
        for
          removeVal ← tokens
            .map: tok ⇒
              repository.deleteAccessTokenWithScopesCIO(
                clientId = clientId,
                appId    = tok.entity.appId
              )
            .sequence
        yield removeVal.sequence map (_ ⇒ ())

      private def deleteRefreshTokenCIO(clientId: Long): TokenAttemptF[Unit] =
        repository.tokenDataSource.deleteToken(
          clientId = clientId,
          appId    = None,
          status   = TokenStatus.Refresh.title
        )

      private def buildToken[T](tokenPrefix: String)(
        newToken: String ⇒ ConnectionIO[Either[InvalidTokenReason, T]]
      ): TokenAttemptF[T] =
        def impl(tokenValueRes: Either[Throwable, String]): TokenAttemptF[T] =
          tokenValueRes
            .map(newToken)
            .sequence
            .map:
              _.sequence.flatMap:
                _.left map (_ ⇒ InvalidTokenReason.GenerationError)

        for
          tokenValueRes ← generateToken[ConnectionIO](tokenPrefix)
          token         ← impl(tokenValueRes)
        yield token

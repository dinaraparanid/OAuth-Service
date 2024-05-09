package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.*
import com.paranid5.auth_service.data.oauth.client.{PostgresAppDataSource, PostgresClientDataSource}
import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenEntity, TokenScope}
import com.paranid5.auth_service.data.oauth.token.error.*
import com.paranid5.auth_service.data.oauth.token.{PostgresTokenDataSource, PostgresTokenScopeDataSource}
import com.paranid5.auth_service.domain.generateToken

import cats.data.ValidatedNec
import cats.effect.IO
import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.syntax.all.*

import io.github.cdimascio.dotenv.Dotenv

final class PostgresOAuthRepository(
  private val transactor:           IOTransactor,
  private val clientDataSource:     PostgresClientDataSource,
  private val appDataSource:        PostgresAppDataSource,
  private val tokenDataSource:      PostgresTokenDataSource,
  private val tokenScopeDataSource: PostgresTokenScopeDataSource
)

object PostgresOAuthRepository:
  private type OAuthAttemptCIO     [T] = ConnectionIO[Either[InvalidOAuthReason, T]]
  private type OAuthValidatedCIO   [T] = ConnectionIO[ValidatedNec[InvalidOAuthReason, T]]
  private type TokenAttemptCIO     [T] = ConnectionIO[Either[InvalidTokenReason, T]]
  private type TokenValidatedCIO   [T] = ConnectionIO[ValidatedNec[InvalidTokenReason, T]]
  private type TokenScopeAttemptCIO[T] = ConnectionIO[Either[InvalidScopeReason, T]]

  given OAuthRepository[IO, PostgresOAuthRepository] with
    override type OAuthAttemptF  [T] = IO[Either[InvalidOAuthReason, T]]
    override type OAuthValidatedF[T] = IO[ValidatedNec[InvalidOAuthReason, T]]
    override type TokenAttemptF  [T] = IO[Either[InvalidTokenReason, T]]
    override type TokenValidatedF[T] = IO[ValidatedNec[InvalidTokenReason, T]]

    extension (repository: PostgresOAuthRepository)
      override def createTables(): IO[Unit] =
        def impl(): ConnectionIO[Unit] =
          for
            _ ← repository.clientDataSource.createTable()
            _ ← repository.appDataSource.createTable()
            _ ← repository.tokenDataSource.createTable()
            _ ← repository.tokenScopeDataSource.createTable()
          yield ()

        impl() transact repository.transactor

      override def getClientWithTokens(
        clientId:     Long,
        clientSecret: String
      ): IO[Option[Client]] =
        def impl: ConnectionIO[Option[Client]] =
          for
            client       ← repository.clientDataSource.getClient(clientId, clientSecret)
            accessTokens ← repository.getClientAccessTokensCIO(clientId)
            refreshToken ← repository.tokenDataSource.getClientRefreshToken(clientId)
          yield client map (Client(_, accessTokens, refreshToken))

        impl transact repository.transactor

      override def isTokenValid(
        clientId:     Long,
        clientSecret: String,
        tokenValue:   String
      ): IO[Boolean] =
        def impl: ConnectionIO[Boolean] =
          for
            clientExists ← repository.isClientExistsCIO(clientId, clientSecret)
            tokenOpt     ← repository.tokenDataSource.getToken(clientId, tokenValue)
          yield clientExists && tokenOpt.isDefined

        impl transact repository.transactor

      override def isClientExits(
        clientId:     Long,
        clientSecret: String
      ): IO[Boolean] =
        repository
          .isClientExistsCIO(clientId, clientSecret)
          .transact(repository.transactor)

      override def insertClient(
        clientId:     Long,
        clientSecret: String
      ): IO[Unit] =
        repository
          .clientDataSource
          .insertClient(clientId, clientSecret)
          .transact(repository.transactor)

      override def deleteClient(clientId: Long): IO[Unit] =
        def impl(): ConnectionIO[Unit] =
          for
            _ ← repository.clientDataSource.deleteClient(clientId)
            _ ← repository.appDataSource.deleteClientApps(clientId)
            _ ← repository.deleteClientTokensWithScopesCIO(clientId)
          yield ()

        impl() transact repository.transactor

      override def getClientApps(clientId: Long): IO[List[AppEntity]] =
        repository
          .appDataSource
          .getClientApps(clientId)
          .transact(repository.transactor)

      override def getApp(
        appId:     Long,
        appSecret: String
      ): IO[Option[AppEntity]] =
        repository
          .appDataSource
          .getApp(appId, appSecret)
          .transact(repository.transactor)

      override def insertApp(
        appId:        Long,
        appSecret:    String,
        appName:      String,
        appThumbnail: Option[String],
        callbackUrl:  Option[String],
        clientId:     Long,
      ): IO[Unit] =
        repository
          .appDataSource
          .insertApp(
            appId        = appId,
            appSecret    = appSecret,
            appName      = appName,
            appThumbnail = appThumbnail,
            callbackUrl  = callbackUrl,
            clientId     = clientId,
          )
          .transact(repository.transactor)

      override def deleteApp(
        clientId:  Long,
        appId:     Long,
        appSecret: String
      ): IO[Unit] =
        def impl(): ConnectionIO[Unit] =
          for
            _ ← repository.deleteAppAccessTokenWithScopesCIO(clientId, appId, appSecret)
            _ ← repository.appDataSource.deleteApp(appId)
          yield ()

        impl() transact repository.transactor

      override def updateApp(
        appId:           Long,
        newAppName:      String,
        newAppThumbnail: Option[String],
        newCallbackUrl:  Option[String],
      ): IO[Unit] =
        repository
          .appDataSource
          .updateApp(
            appId           = appId,
            newAppName      = newAppName,
            newAppThumbnail = newAppThumbnail,
            newCallbackUrl  = newCallbackUrl
          )
          .transact(repository.transactor)

      override def getClientAccessTokens(clientId: Long): IO[List[AccessToken]] =
        repository
          .getClientAccessTokensCIO(clientId)
          .transact(repository.transactor)

      override def newAccessToken(
        refreshToken:     RefreshToken,
        accessTokenTitle: String,
        lifeSeconds:      Option[Long],
        scopes:           List[TokenScope]
      ): TokenAttemptF[AccessToken] =
        def impl(
          tokenValue: String
        ): TokenAttemptCIO[AccessToken] =
          for
            token ← repository.tokenDataSource.newAccessToken(
              refreshToken = refreshToken,
              title        = accessTokenTitle,
              lifeSeconds  = lifeSeconds,
              tokenValue   = tokenValue
            )

            _ ← repository.tokenScopeDataSource.addScopesToToken(
              clientId         = refreshToken.clientId,
              accessTokenTitle = accessTokenTitle,
              scopes           = scopes
            )
          yield token map (t ⇒ AccessToken(entity = t, scopes = scopes))

        for
          tokenValueRes ← generateToken(tokenPrefix = accessTokenTitle)
          token         ← transactToken(tokenValueRes)(impl)
        yield token

      override def newRefreshToken(
        clientId:     Long,
        clientSecret: String
      ): TokenAttemptF[RefreshToken] =
        def impl(tokenValue: String): TokenAttemptCIO[RefreshToken] =
          for token ← repository.tokenDataSource.newRefreshToken(
            clientId     = clientId,
            clientSecret = clientSecret,
            tokenValue   = tokenValue
          ) yield token

        for
          tokenValueRes ← generateToken(tokenPrefix = f"$clientId")
          token         ← transactToken(tokenValueRes)(impl)
        yield token

      override def deleteAccessTokenWithScopes(
        clientId: Long,
        title:    String
      ): IO[ValidatedNec[InvalidOAuthReason, Unit]] =
        repository
          .deleteAccessTokenWithScopesCIO(clientId, title)
          .transact(repository.transactor)

      override def deleteClientTokensWithScopes(
        clientId: Long
      ): IO[ValidatedNec[InvalidOAuthReason, Unit]] =
        repository
          .deleteClientTokensWithScopesCIO(clientId)
          .transact(repository.transactor)

      private def isClientExistsCIO(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Boolean] =
        for clientOpt ← repository.clientDataSource.getClient(clientId, clientSecret)
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
        def retrieveAppToken(
          clientApp: Option[AppEntity]
        ): ConnectionIO[Option[TokenEntity]] =
          clientApp
            .map: app ⇒
              repository
                .tokenDataSource
                .getTokenByTitle(clientId, app.appName)
            .sequence
            .map(_.flatten)

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
          app         ← repository.appDataSource.getApp(appId, appSecret)
          token       ← retrieveAppToken(app)
          accessToken ← retrieveTokenWithScope(token)
        yield accessToken

      private def deleteAccessTokenWithScopesCIO(
        clientId: Long,
        title:    String,
      ): ConnectionIO[ValidatedNec[InvalidOAuthReason, Unit]] =
        def removeScopes(): TokenScopeAttemptCIO[Unit] =
          repository
            .tokenScopeDataSource
            .removeAllScopesFromToken(
              clientId         = clientId,
              accessTokenTitle = title
            )

        def removeToken(): TokenAttemptCIO[Unit] =
          repository.tokenDataSource.deleteToken(clientId, Option(title))

        for
          rmScopesRes ← removeScopes()
          rmTokenRes  ← removeToken()

          rmScopesVal: ValidatedNec[InvalidOAuthReason, Unit] = rmScopesRes.toValidatedNec
          rmTokenVal:  ValidatedNec[InvalidOAuthReason, Unit] = rmTokenRes.toValidatedNec
        yield (rmScopesVal, rmTokenVal) mapN ((_, _) ⇒ ())

      private def deleteClientTokensWithScopesCIO(clientId: Long): OAuthValidatedCIO[Unit] =
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
      ): OAuthValidatedCIO[Unit] =
        for
          accessToken  ← repository.getAppAccessTokenCIO(clientId, appId, appSecret)
          accessTokens = accessToken map (List(_)) getOrElse Nil
          accessTokVal ← deleteAccessTokensWithScopesCIO(clientId, accessTokens)
        yield accessTokVal

      private def deleteAccessTokensWithScopesCIO(
        clientId: Long,
        tokens:   List[AccessToken]
      ): OAuthValidatedCIO[Unit] =
        for
          removeVal ← tokens
            .map: tok ⇒
              repository.deleteAccessTokenWithScopesCIO(
                clientId = clientId,
                title    = tok.entity.title getOrElse ""
              )
            .sequence
        yield removeVal.sequence map (_ ⇒ ())

      private def deleteRefreshTokenCIO(clientId: Long): TokenAttemptCIO[Unit] =
        repository.tokenDataSource.deleteToken(clientId, None)

      private def transactToken[T](tokenValueRes: Either[Throwable, String])(
        newToken: String ⇒ TokenAttemptCIO[T]
      ): TokenAttemptF[T] =
        tokenValueRes
          .map(newToken >>> (_ transact repository.transactor))
          .sequence
          .map:
            _.sequence.flatMap:
              _.left map (_ ⇒ InvalidTokenReason.GenerationError)

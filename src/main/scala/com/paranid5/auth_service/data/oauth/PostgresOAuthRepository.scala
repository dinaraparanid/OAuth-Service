package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.oauth.client.{PostgresAppDataSource, PostgresClientDataSource}
import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenScope}
import com.paranid5.auth_service.data.oauth.token.error.*
import com.paranid5.auth_service.data.oauth.token.{PostgresTokenDataSource, PostgresTokenScopeDataSource}
import com.paranid5.auth_service.token.generateToken

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
  private val PostgresDbUrl      = "POSTGRES_DB_URL"
  private val PostgresDbUser     = "POSTGRES_DB_USER"
  private val PostgresDbPassword = "POSTGRES_DB_PASSWORD"

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

    override infix def connect(dotenv: Dotenv): PostgresOAuthRepository =
      val transactor = Transactor.fromDriverManager[IO](
        driver       = "org.postgresql.Driver",
        url          = dotenv `get` PostgresDbUrl,
        user         = dotenv `get` PostgresDbUser,
        password     = dotenv `get` PostgresDbPassword,
        logHandler   = None
      )

      PostgresOAuthRepository(
        transactor           = transactor,
        clientDataSource     = PostgresClientDataSource(),
        appDataSource        = PostgresAppDataSource(),
        tokenDataSource      = PostgresTokenDataSource(),
        tokenScopeDataSource = PostgresTokenScopeDataSource()
      )

    extension (repository: PostgresOAuthRepository)
      override def getClientWithTokens(
        clientId:     Long,
        clientSecret: String
      ): IO[Option[Client]] =
        def impl: ConnectionIO[Option[Client]] =
          for
            client       ← repository.clientDataSource.getClient(clientId, clientSecret)
            accessTokens ← repository getClientAccessTokensCIO clientId
            refreshToken ← repository.tokenDataSource getClientRefreshToken clientId
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

      override infix def deleteClient(clientId: Long): IO[Unit] =
        def impl(): ConnectionIO[Unit] =
          for
            _ ← repository.clientDataSource deleteClient clientId
            _ ← repository.appDataSource deleteClientApps clientId
          yield ()

        // TODO: удалить со всем

        repository
          .clientDataSource
          .deleteClient(clientId)
          .transact(repository.transactor)

      override infix def getClientApps(clientId: Long): IO[List[AppEntity]] =
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

      override infix def deleteApp(appId: Long): IO[Unit] =
        repository
          .appDataSource
          .deleteApp(appId)
          .transact(repository.transactor)

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

      override infix def getClientAccessTokens(clientId: Long): IO[List[AccessToken]] =
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

            b ← repository.tokenScopeDataSource.addScopesToToken(
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

      override infix def deleteClientTokensWithScopes(
        clientId: Long
      ): IO[ValidatedNec[InvalidOAuthReason, Unit]] =
        def deleteRefreshTokenCIO(): TokenAttemptCIO[Unit] =
          repository.tokenDataSource.deleteToken(clientId, None)

        def deleteAccessTokensWithScopesCIO(): OAuthValidatedCIO[Unit] =
          for
            tokens    ← repository getClientAccessTokensCIO clientId
            removeVal ← tokens
              .map: tok ⇒
                repository.deleteAccessTokenWithScopesCIO(
                  clientId = clientId,
                  title = tok.entity.title getOrElse ""
                )
              .sequence
          yield removeVal.sequence map (_ ⇒ ())

        def impl(): OAuthValidatedCIO[Unit] =
          for
            refreshTokRes ← deleteRefreshTokenCIO()
            accessTokVal  ← deleteAccessTokensWithScopesCIO()
            refreshTokVal = refreshTokRes.toValidatedNec
          yield (refreshTokVal, accessTokVal) mapN ((_, _) ⇒ ())

        impl() transact repository.transactor

      private def isClientExistsCIO(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Boolean] =
        for clientOpt ← repository.clientDataSource.getClient(clientId, clientSecret)
          yield clientOpt.isDefined

      private infix def getClientAccessTokensCIO(
        clientId: Long
      ): ConnectionIO[List[AccessToken]] =
        for
          accToksE     ← repository.tokenDataSource.getClientAccessTokens(clientId)
          accessTokens ← repository.tokenScopeDataSource.getAccessTokensWithScopes(accToksE)
        yield accessTokens

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

      private def transactToken[T](tokenValueRes: Either[Throwable, String])(
        newToken: String ⇒ TokenAttemptCIO[T]
      ): TokenAttemptF[T] =
        tokenValueRes
          .map(newToken >>> (_ transact repository.transactor))
          .sequence
          .map:
            _.sequence.flatMap:
              _.left map (_ ⇒ InvalidTokenReason.GenerationError)

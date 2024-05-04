package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.oauth.client.PostgresClientDataSource
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken, TokenScope}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.data.oauth.token.{PostgresTokenDataSource, PostgresTokenScopeDataSource}
import com.paranid5.auth_service.token.generateToken

import cats.effect.IO
import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.syntax.all.*

import io.github.cdimascio.dotenv.Dotenv

final class PostgresOAuthRepository(
  private val transactor:           IOTransactor,
  private val clientDataSource:     PostgresClientDataSource,
  private val tokenDataSource:      PostgresTokenDataSource,
  private val tokenScopeDataSource: PostgresTokenScopeDataSource
)

object PostgresOAuthRepository:
  private val PostgresDbUrl      = "POSTGRES_DB_URL"
  private val PostgresDbUser     = "POSTGRES_DB_USER"
  private val PostgresDbPassword = "POSTGRES_DB_PASSWORD"

  private type TokenAttemptCIO[T] = ConnectionIO[Either[InvalidTokenReason, T]]

  given OAuthRepository[IO, PostgresOAuthRepository] with
    override type TokenAttemptF[T]  = IO[Either[InvalidTokenReason, T]]

    override def connect(dotenv: Dotenv): PostgresOAuthRepository =
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

      override def storeClient(
        clientId:     Long,
        clientSecret: String
      ): IO[Unit] =
        repository
          .clientDataSource
          .storeClient(clientId, clientSecret)
          .transact(repository.transactor)

      override def deleteClient(clientId: Long): IO[Unit] =
        repository
          .clientDataSource
          .deleteClient(clientId)
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

            b ← repository.tokenScopeDataSource.addScopesToToken(
              clientId         = refreshToken.clientId,
              accessTokenTitle = accessTokenTitle,
              scopes           = scopes
            )
          yield token.map(t ⇒ AccessToken(entity = t, scopes = scopes))

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

      private def transactToken[T](tokenValueRes: Either[Throwable, String])(
        newToken: String ⇒ TokenAttemptCIO[T]
      ): TokenAttemptF[T] =
        tokenValueRes
          .map(newToken >>> (_.transact(repository.transactor)))
          .sequence
          .map:
            _.sequence.flatMap:
              _.left.map(_ ⇒ InvalidTokenReason.GenerationError)

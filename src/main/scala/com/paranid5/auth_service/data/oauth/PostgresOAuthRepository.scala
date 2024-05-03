package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.oauth.client.PostgresClientDataSource
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken}
import com.paranid5.auth_service.data.oauth.token.{InvalidTokenReason, PostgresTokenDataSource}

import cats.effect.IO

import doobie.util.transactor.Transactor

import io.github.cdimascio.dotenv.Dotenv

private val PostgresDbUrl      = "POSTGRES_DB_URL"
private val PostgresDbUser     = "POSTGRES_DB_USER"
private val PostgresDbPassword = "POSTGRES_DB_PASSWORD"

final class PostgresOAuthRepository(
  private val transactor:       IOTransactor,
  private val clientDataSource: PostgresClientDataSource,
  private val tokenDataSource:  PostgresTokenDataSource
)

object PostgresOAuthRepository:
  given OAuthRepository[IO, PostgresOAuthRepository] with
    override def connect(dotenv: Dotenv): PostgresOAuthRepository =
      val transactor = Transactor.fromDriverManager[IO](
        driver       = "org.postgresql.Driver",
        url          = dotenv `get` PostgresDbUrl,
        user         = dotenv `get` PostgresDbUser,
        password     = dotenv `get` PostgresDbPassword,
        logHandler   = None
      )

      PostgresOAuthRepository(
        transactor       = transactor,
        clientDataSource = PostgresClientDataSource(),
        tokenDataSource  = PostgresTokenDataSource()
      )

    extension (repository: PostgresOAuthRepository)
      override def getClientWithTokens(clientId: Long): IO[Client] = ???

      override def isAccessTokenValid(
        clientId:     Long,
        clientSecret: String,
        accessToken:  String
      ): IO[Boolean] = ???

      override def isRefreshTokenValid(
        clientId:     Long,
        clientSecret: String,
        refreshToken: String
      ): IO[Boolean] = ???

      override def isClientExits(
        clientId:     Long,
        clientSecret: String
      ): IO[Boolean] = ???

      override def storeClient(
        clientId:     Long,
        clientSecret: String
      ): IO[Unit] = ???

      override def deleteClient(clientId: Long): IO[Unit] = ???

      override def userAccessTokens(userId: Long): IO[List[AccessToken]] = ???

      override def newAccessToken(
        refreshToken: RefreshToken
      ): IO[Either[InvalidTokenReason, AccessToken]] = ???

      override def newRefreshToken(
        clientId:     Long,
        clientSecret: String
      ): IO[Option[RefreshToken]] = ???


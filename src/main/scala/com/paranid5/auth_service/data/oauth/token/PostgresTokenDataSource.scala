package com.paranid5.auth_service.data.oauth.token

import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity, TokenStatus, isActual}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.data.ops.*

import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment

final class PostgresTokenDataSource

object PostgresTokenDataSource:
  private val RefreshTokenAliveTime: Long = 1000 * 60 * 60 * 24 // 1 day

  given TokenDataSource[ConnectionIO, PostgresTokenDataSource] with
    override type TokenAttemptF[T] = ConnectionIO[Either[InvalidTokenReason, T]]

    extension (source: PostgresTokenDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "Token" (
          client_id INTEGER NOT NULL REFERENCES "Client"(client_id),
          title TEXT,
          value TEXT NOT NULL,
          life_seconds INTEGER,
          created_at INTEGER NOT NULL,
          status VARCHAR(10) NOT NULL,
          PRIMARY KEY (client_id, title, value)
        )
        """.effect

      override def getClientAccessTokens(clientId: Long): ConnectionIO[List[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND status = "access"
        """.list[TokenEntity]

      override def getClientRefreshToken(clientId: Long): ConnectionIO[Option[RefreshToken]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND status = "refresh"
        """.option[RefreshToken]

      override def getToken(
        clientId:     Long,
        tokenValue:   String
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND value = $tokenValue
        """.option[TokenEntity]

      override def getTokenByTitle(
        clientId:   Long,
        tokenTitle: String
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND title = $tokenTitle
        """.option[TokenEntity]

      override def newAccessToken(
        refreshToken: RefreshToken,
        title:        String,
        lifeSeconds:  Option[Long],
        tokenValue:   String,
      ): TokenAttemptF[TokenEntity] =
        def impl(tokenValue: String): TokenAttemptF[TokenEntity] =
          newToken(
            clientId    = refreshToken.clientId,
            title       = Option(title),
            value       = tokenValue,
            lifeSeconds = lifeSeconds,
            createdAt   = System.currentTimeMillis,
            status      = TokenStatus.Access.title
          )

        for
          _     ← isTokenValid(refreshToken)
          token ← impl(tokenValue)
        yield token

      override def newRefreshToken(
        clientId:     Long,
        clientSecret: String,
        tokenValue:   String,
      ): TokenAttemptF[RefreshToken] =
        newToken(
          clientId    = clientId,
          title       = None,
          value       = tokenValue,
          lifeSeconds = Option(RefreshTokenAliveTime),
          createdAt   = System.currentTimeMillis,
          status      = TokenStatus.Refresh.title
        )

      override def isTokenValid(token: TokenEntity): ConnectionIO[Either[InvalidTokenReason, Unit]] =
        val TokenEntity(clientId, title, value, lifeSeconds, createdAt, status) = token
        for foundTokenOpt ← getToken(clientId = clientId, title = title, value = value)
          yield foundTokenOpt
            .toRight(InvalidTokenReason.NotFound)
            .flatMap: token ⇒
              if token.isActual then Right(())
              else Left(InvalidTokenReason.Expired)

      override def getToken(
        clientId: Long,
        title:    Option[String],
        value:    String
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND title = $title AND value = $value
        """.option[TokenEntity]

      override def deleteToken(
        clientId: Long,
        title:    Option[String]
      ): TokenAttemptF[Unit] =
        sql"""
        DELETE FROM "Token"
        WHERE client_id = $clientId AND title = $title
        """.attemptDelete

      private def newToken(
        clientId:    Long,
        title:       Option[String],
        value:       String,
        lifeSeconds: Option[Long],
        createdAt:   Long,
        status:      String
      ): TokenAttemptF[TokenEntity] =
        sql"""
        INSERT INTO "Token" (client_id, title, value, life_seconds, created_at, status)
        VALUES ($clientId, $title, $value, $lifeSeconds, $createdAt, $status)
        """.attemptInsert:
          TokenEntity(clientId, title, value, lifeSeconds, createdAt, status)

  extension (query: Fragment)
    private def attemptInsert(
      token: TokenEntity
    ): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
      query.update.run.attempt.map: res ⇒
        res.bimap(
          fa = _ ⇒ InvalidTokenReason.GenerationError,
          fb = _ ⇒ token,
        )

    private def attemptDelete: ConnectionIO[Either[InvalidTokenReason, Unit]] =
      query.update.run.attempt.map: res ⇒
        res.bimap(
          fa = _ ⇒ InvalidTokenReason.NotFound,
          fb = _ ⇒ ()
        )


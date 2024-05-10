package com.paranid5.auth_service.data.oauth.token

import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity, TokenStatus, isActual}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.data.ops.*
import cats.syntax.all.*
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason.NotFound
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
          token_id SERIAL PRIMARY KEY,
          client_id INTEGER NOT NULL REFERENCES "Client"(client_id) ON DELETE CASCADE,
          app_id INTEGER REFERENCES "App"(app_id) ON DELETE CASCADE,
          value TEXT NOT NULL,
          life_seconds BIGINT,
          created_at BIGINT NOT NULL,
          status VARCHAR(10) NOT NULL,
          UNIQUE(client_id, app_id, value)
        )
        """.effect

      override def getClientAccessTokens(clientId: Long): ConnectionIO[List[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND status = 'access'
        """.list[TokenEntity]

      override def getPlatformClientAccessToken(
        clientId: Long
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND status = 'access' AND app_id IS NULL
        """.option[TokenEntity]

      override def getClientRefreshToken(clientId: Long): ConnectionIO[Option[RefreshToken]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND status = 'refresh'
        """.option[RefreshToken]

      override def findToken(
        clientId:     Long,
        tokenValue:   String
      ): TokenAttemptF[TokenEntity] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND value = $tokenValue
        """.option[TokenEntity] map (_ toRight NotFound)

      override def retrieveToken(tokenValue: String): TokenAttemptF[TokenEntity] =
        sql"""
        SELECT * FROM "Token"
        WHERE value = $tokenValue
        """.option[TokenEntity] map (_ toRight NotFound)

      override def getTokenByAppId(
        clientId: Long,
        appId:    Long
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE client_id = $clientId AND app_id = $appId
        """.option[TokenEntity]

      override def newAppAccessToken(
        refreshToken: RefreshToken,
        appId:        Long,
        lifeSeconds:  Option[Long],
        tokenValue:   String,
      ): TokenAttemptF[TokenEntity] =
        def impl(tokenValue: String): TokenAttemptF[TokenEntity] =
          newToken(
            clientId    = refreshToken.clientId,
            appId       = Option(appId),
            value       = tokenValue,
            lifeSeconds = lifeSeconds,
            createdAt   = System.currentTimeMillis,
            status      = TokenStatus.Access.title
          )

        for
          _     ← isTokenValid(refreshToken)
          token ← impl(tokenValue)
        yield token

      override def newPlatformAccessToken(
        refreshToken: RefreshToken,
        lifeSeconds:  Option[Long],
        tokenValue:   String,
      ): TokenAttemptF[TokenEntity] =
        def impl(tokenValue: String): TokenAttemptF[TokenEntity] =
          newToken(
            clientId    = refreshToken.clientId,
            appId       = None,
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
          appId       = None,
          value       = tokenValue,
          lifeSeconds = Option(RefreshTokenAliveTime),
          createdAt   = System.currentTimeMillis,
          status      = TokenStatus.Refresh.title
        )

      override def isTokenValid(token: TokenEntity): ConnectionIO[Either[InvalidTokenReason, Unit]] =
        val TokenEntity(_, clientId, appId, value, lifeSeconds, createdAt, status) = token
        for foundTokenOpt ← getToken(clientId = clientId, appId = appId, value = value)
          yield foundTokenOpt
            .toRight(InvalidTokenReason.NotFound)
            .flatMap: token ⇒
              if token.isActual then Right(())
              else Left(InvalidTokenReason.Expired)

      override def getToken(
        clientId: Long,
        appId:    Option[Long],
        value:    String
      ): ConnectionIO[Option[TokenEntity]] =
        def query: Fragment =
          appId match
            case Some(id) ⇒
              sql"""
              SELECT * FROM "Token"
              WHERE client_id = $clientId AND app_id = $id AND value = $value
              """

            case None ⇒
              sql"""
              SELECT * FROM "Token"
              WHERE client_id = $clientId AND app_id IS NULL AND value = $value
              """

        query.option[TokenEntity]

      override def deleteToken(
        clientId: Long,
        appId:    Option[Long],
        status:   String,
      ): TokenAttemptF[Unit] =
        def query: Fragment =
          appId match
            case Some(id) ⇒
              sql"""
              DELETE FROM "Token"
              WHERE client_id = $clientId AND app_id = $id AND status = $status
              """

            case None ⇒
              sql"""
              DELETE FROM "Token"
              WHERE client_id = $clientId AND app_id IS NULL AND status = $status
              """

        query.attemptDelete

      private def newToken(
        clientId:    Long,
        appId:       Option[Long],
        value:       String,
        lifeSeconds: Option[Long],
        createdAt:   Long,
        status:      String
      ): TokenAttemptF[TokenEntity] =
        sql"""
        INSERT INTO "Token" (client_id, app_id, value, life_seconds, created_at, status)
        VALUES ($clientId, $appId, $value, $lifeSeconds, $createdAt, $status)
        RETURNING token_id
        """.attemptInsert: tokenId ⇒
          TokenEntity(tokenId, clientId, appId, value, lifeSeconds, createdAt, status)

  extension (query: Fragment)
    private def attemptInsert(
      token: Long ⇒ TokenEntity
    ): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
      query.serialId.attempt.map: res ⇒
        res.bimap(
          fa = x ⇒ {
            x.printStackTrace()
            InvalidTokenReason.GenerationError
          },
          fb = token,
        )

    private def attemptDelete: ConnectionIO[Either[InvalidTokenReason, Unit]] =
      query.update.run.attempt.map: res ⇒
        res.bimap(
          fa = _ ⇒ InvalidTokenReason.NotFound,
          fb = _ ⇒ ()
        )


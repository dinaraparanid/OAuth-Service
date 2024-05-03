package com.paranid5.auth_service.data.oauth.token

import com.paranid5.auth_service.data.oauth.token.entity.{RefreshToken, TokenEntity, TokenStatus, isActual}

import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

private val RefreshTokenAliveTime = 2_592_000_000L // 30 days

final class PostgresTokenDataSource

object PostgresTokenDataSource:
  given TokenDataSource[ConnectionIO, PostgresTokenDataSource] with
    extension (source: PostgresTokenDataSource)
      override def userAccessTokens(userId: Long): ConnectionIO[List[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE user_id = $userId AND status = "access"
        """.query[TokenEntity].to[List]

      override def newAccessToken(
        refreshToken: RefreshToken,
        title:        String,
        lifeSeconds:  Option[Long],
        tokenValue:   String,
      ): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
        def impl(tokenValue: String): ConnectionIO[Either[InvalidTokenReason, TokenEntity]] =
          newToken(
            userId      = refreshToken.userId,
            title       = Option(title),
            value       = tokenValue,
            lifeSeconds = lifeSeconds,
            createdAt   = System.currentTimeMillis,
            status      = TokenStatus.Access.title
          ).map(_ toRight InvalidTokenReason.GenerationError)

        for
          _     ← isTokenValid(refreshToken)
          token ← impl(tokenValue)
        yield token

      override def newRefreshToken(
        clientId:     Long,
        clientSecret: String,
        tokenValue:   String,
      ): ConnectionIO[Either[InvalidTokenReason, RefreshToken]] =
        newToken(
          userId      = clientId,
          title       = None,
          value       = tokenValue,
          lifeSeconds = Option(RefreshTokenAliveTime),
          createdAt   = System.currentTimeMillis,
          status      = TokenStatus.Refresh.title
        ).map(_ toRight InvalidTokenReason.GenerationError)


      override def isTokenValid(token: TokenEntity): ConnectionIO[Either[InvalidTokenReason, Unit]] =
        val TokenEntity(userId, title, value, lifeSeconds, createdAt, status) = token
        for foundTokenOpt ← getToken(userId = userId, title = title, value = value)
          yield foundTokenOpt
            .toRight(InvalidTokenReason.NotFound)
            .flatMap: token ⇒
              if token.isActual then Right(())
              else Left(InvalidTokenReason.Expired)

      override def getToken(
        userId: Long,
        title:  Option[String],
        value:  String
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        SELECT * FROM "Token"
        WHERE user_id = $userId AND title = $title AND value = $value
        """
          .query[TokenEntity]
          .to[List]
          .map(_.headOption)

      private def newToken(
        userId:      Long,
        title:       Option[String],
        value:       String,
        lifeSeconds: Option[Long],
        createdAt:   Long,
        status:      String
      ): ConnectionIO[Option[TokenEntity]] =
        sql"""
        INSERT INTO "Token" (user_id, title, value, life_seconds, created_at, status)
        VALUES ($userId, $title, $value, $lifeSeconds, $createdAt, $status)
        """.update.run.attempt.map: res ⇒
          res
            .map(_ ⇒ TokenEntity(userId, title, value, lifeSeconds, createdAt, status))
            .toOption

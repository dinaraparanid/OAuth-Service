package com.paranid5.auth_service.data.oauth.token

import com.paranid5.auth_service.data.oauth.token.entity.TokenScopeRelation
import com.paranid5.auth_service.data.oauth.token.error.InvalidScopeReason
import com.paranid5.auth_service.data.ops.*

import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment

final class PostgresTokenScopeDataSource

object PostgresTokenScopeDataSource:
  given TokenScopeDataSource[ConnectionIO, PostgresTokenScopeDataSource] with
    override type TokenScopeAttemptF[T] = ConnectionIO[Either[InvalidScopeReason, T]]

    extension (source: PostgresTokenScopeDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "Token_Scope" (
          client_id INTEGER NOT NULL REFERENCES "Token"(client_id) ON DELETE CASCADE,
          token_title TEXT NOT NULL REFERENCES "Token"(title) ON DELETE CASCADE,
          scope TEXT NOT NULL,
          PRIMARY KEY (client_id, token_title, scope)
        )
        """.effect

      override def getTokenScopes(
        clientId:         Long,
        accessTokenTitle: String
      ): ConnectionIO[List[TokenScopeRelation]] =
        sql"""
        SELECT * FROM "Token_Scope"
        WHERE client_id = $clientId AND token_title = $accessTokenTitle
        """.list[TokenScopeRelation]

      override def addScopeToToken(
        clientId:         Long,
        accessTokenTitle: String,
        scope:            String
      ): ConnectionIO[Either[InvalidScopeReason, Unit]] =
        sql"""
        INSERT INTO "Token_Scope" (client_id, token_title, scope)
        VALUES ($clientId, $accessTokenTitle, $scope)
        """.attemptInsert

      override def removeScopeFromToken(
        clientId:         Long,
        accessTokenTitle: String,
        scope:            String
      ): TokenScopeAttemptF[Unit] =
        sql"""
        DELETE FROM "Token_Scope"
        WHERE client_id = $clientId
         AND token_title = $accessTokenTitle
         AND scope = $scope
        """.attemptDelete

      override def removeAllScopesFromToken(
        clientId:         Long,
        accessTokenTitle: String,
      ): TokenScopeAttemptF[Unit] =
        sql"""
        DELETE FROM "Token_Scope"
        WHERE client_id = $clientId AND token_title = $accessTokenTitle
        """.attemptDelete

  extension (query: Fragment)
    private def attemptInsert: ConnectionIO[Either[InvalidScopeReason, Unit]] =
      query.update.run.attempt.map: res ⇒
        res.bimap(
          fa = _ ⇒ InvalidScopeReason.ScopeAlreadyApplied,
          fb = _ ⇒ ()
        )

    private def attemptDelete: ConnectionIO[Either[InvalidScopeReason, Unit]] =
      query.update.run.attempt.map: res ⇒
        res.bimap(
          fa = _ ⇒ InvalidScopeReason.ScopeNotApplied,
          fb = _ ⇒ ()
        )

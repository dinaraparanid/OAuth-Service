package com.paranid5.auth_service.data.oauth.token

import cats.Applicative
import cats.syntax.all.*

import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, TokenEntity, TokenScope, TokenScopeRelation}
import com.paranid5.auth_service.data.oauth.token.error.InvalidScopeReason

trait TokenScopeDataSource[F[_] : Applicative, S]:
  type TokenScopeAttemptF[T] = F[Either[InvalidScopeReason, T]]

  extension (source: S)
    def getTokenScopes(
      clientId:         Long,
      accessTokenTitle: String,
    ): F[List[TokenScopeRelation]]

    def getAccessTokensWithScopes(
      accessTokens: List[TokenEntity]
    ): F[List[AccessToken]] =
      def buildAccessToken(token: TokenEntity): F[AccessToken] =
        for tokScopesRel ← source.getTokenScopes(token.clientId, token.title getOrElse "")
          yield AccessToken(
            entity = token,
            scopes = tokScopesRel map (r ⇒ TokenScope(r.scope))
          )

      accessTokens.map(buildAccessToken).sequence

    def addScopeToToken(
      clientId:         Long,
      accessTokenTitle: String,
      scope:            String
    ): F[Either[InvalidScopeReason, Unit]]

    def addScopesToToken(
      clientId:         Long,
      accessTokenTitle: String,
      scopes:           List[TokenScope]
    ): F[List[Either[InvalidScopeReason, Unit]]] =
      scopes
        .map(s ⇒ source.addScopeToToken(clientId, accessTokenTitle, s.value))
        .sequence

    def removeScopeFromToken(
      clientId:         Long,
      accessTokenTitle: String,
      scope:            String
    ): TokenScopeAttemptF[Unit]

    def removeAllScopesFromToken(
      clientId:         Long,
      accessTokenTitle: String,
    ): TokenScopeAttemptF[Unit]

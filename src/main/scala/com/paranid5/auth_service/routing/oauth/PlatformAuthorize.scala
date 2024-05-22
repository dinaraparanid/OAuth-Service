package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.AccessToken
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.entity.AuthorizeRequest
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Validates access token for authorization for platform.
 * Redirects either to provided callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/authorize?client_id=123&redirect_url=https://...
 *
 * ==Body==
 * {{{
 *   {
 *     "token": "abcdef"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 *
 * 2. [[NotFound]] - "Token was not found"
 *
 * 3. [[Forbidden]] - "Token has expired"
 *
 * 5. [[Found]] with provided callback url or with [[DefaultRedirect]]
 */

private def onPlatformAuthorize(
  query:       Request[IO],
  clientId:    Long,
  redirectUrl: Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def retrieveToken(request: AuthorizeRequest): ConnectionIO[IO[Response[IO]]] =
      for
        tokenOpt ← oauthRepository.getPlatformClientAccessToken(clientId)
        response ← processToken(request.token, tokenOpt)
      yield response

    def processToken(
      requestToken:      String,
      retrievedTokenOpt: Option[AccessToken]
    ): ConnectionIO[IO[Response[IO]]] =
      retrievedTokenOpt.foldTraverseR(
        ifEmpty = tokenNotFound)(
        f       = validateToken
      )

    def validateToken(token: AccessToken): ConnectionIO[IO[Response[IO]]] =
      for isValid ← oauthRepository.isTokenValid(
        clientId   = token.entity.clientId,
        tokenValue = token.entity.value
      ) yield isValid.fold(
        fa = invalidToken,
        fb = _ ⇒ redirectToCallbackUrl(redirectUrl getOrElse DefaultRedirect)
      )

    processRequest(query.attemptAs[AuthorizeRequest])(retrieveToken) run appModule

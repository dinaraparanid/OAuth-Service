package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.{AppEntity, ClientEntity}
import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.{SignInRequest, matches}
import com.paranid5.auth_service.routing.oauth.entity.SignInResponse

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

private def userSignedIn(
  clientId:     Long,
  clientSecret: String,
  accessToken:  TokenEntity,
  refreshToken: TokenEntity,
  redirectUrl:  String,
): IO[Response[IO]] =
  Found:
    SignInResponse(
      clientId     = clientId,
      clientSecret = clientSecret,
      accessToken  = accessToken,
      refreshToken = refreshToken,
      redirectUrl  = redirectUrl,
    ).asJson

/**
 * Sign in on client app.
 * Retrieves client credentials of the given user.
 * Removes old refresh and access tokens for this app (if any)
 * and generates new ones.
 *
 * ==Route==
 * POST /oauth/sign_in?app_id=123&app_secret=secret&redirect_url=https://...
 *
 * ==Body==
 * {{{
 *   {
 *     "email":    "test@gmail.com",
 *     "password": "qwerty"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "User with provided email was not found"
 *
 * 3. [[NotFound]] - "User with provided email and password was not found"
 *
 * 4. [[NotFound]] - "App was not found"
 *
 * 5. [[Found]] with credentials body:
 * {{{
 *   {
 *     "client_id":     123,
 *     "client_secret": "abcdefghij", // 10-th length string
 *     "access_token":  {
 *       "client_id":    123,
 *       "title":        "App Title",
 *       "value":        "abcdef", // 45-th length string
 *       "life_seconds": 100,
 *       "created_at":   100, // time since January 1, 1970 UTC
 *       "status":       "access"
 *     },
 *     "refresh_token":  {
 *       "client_id":    123,
 *       "title":        null,
 *       "value":        "abcdef", // 45-th length string
 *       "life_seconds": 100,
 *       "created_at":   100, // time since January 1, 1970 UTC
 *       "status":       "refresh"
 *     },
 *     "redirect_url": "https://..."
 *   }
 * }}}
 */

private def onSignIn(
  query:      Request[IO],
  appId:       Long,
  appSecret:   String,
  redirectUrl: Option[String],
): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, SignInRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveUserData)
        response   ← responseIO
      yield response

    def retrieveUserData(request: SignInRequest): IO[Response[IO]] =
      for
        user     ← userRepository.getUserByEmail(request.email)
        response ← processUserData(request, user)
      yield response

    def processUserData(
      request:   SignInRequest,
      foundUser: Option[User]
    ): IO[Response[IO]] =
      foundUser.fold(
        ifEmpty = wrongEmail)(
        f       = validateUser(request, _)
      )

    def validateUser(
      request:   SignInRequest,
      foundUser: User
    ): IO[Response[IO]] =
      if request matches foundUser then retrieveCredentials(request, foundUser)
      else wrongPassword

    def retrieveCredentials(
      request:   SignInRequest,
      foundUser: User
    ): IO[Response[IO]] =
      for
        clientOpt ← oauthRepository.getClient(foundUser.userId)
        response  ← clientOpt.fold(
          ifEmpty = somethingWentWrong)(
          f       = retrieveApp
        )
      yield response

    def retrieveApp(client: ClientEntity): IO[Response[IO]] =
      for
        app      ← oauthRepository.getApp(appId, appSecret)
        response ← app.fold(
          ifEmpty = appNotFound)(
          f       = removeOldTokens(client, _)
        )
      yield response

    def removeOldTokens(
      client: ClientEntity,
      app:    AppEntity,
    ): IO[Response[IO]] =
      for
        _ ← oauthRepository.deleteRefreshToken(client.clientId)

        _ ← oauthRepository.deleteAccessTokenWithScopes(
          clientId = client.clientId,
          title    = app.appName
        )

        response ← generateRefreshToken(client, app)
      yield response

    def generateRefreshToken(
      client: ClientEntity,
      app:    AppEntity,
    ): IO[Response[IO]] =
      for
        refreshTokenRes ← oauthRepository.newRefreshToken(
          clientId     = client.clientId,
          clientSecret = client.clientSecret
        )

        response ← refreshTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = generateAccessToken(client, app, _)
        )
      yield response

    def generateAccessToken(
      client:       ClientEntity,
      app:          AppEntity,
      refreshToken: TokenEntity,
    ): IO[Response[IO]] =
      for
        accessTokenRes ← oauthRepository.newAccessToken(
          refreshToken     = refreshToken,
          accessTokenTitle = app.appName,
        )

        redirect = redirectUrl getOrElse (app.callbackUrl getOrElse DefaultRedirect)

        response ← accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = accessToken ⇒
            userSignedIn(
              clientId     = client.clientId,
              clientSecret = client.clientSecret,
              accessToken  = accessToken.entity,
              refreshToken = refreshToken,
              redirectUrl  = redirect
            )
        )
      yield response

    processRequest(query.attemptAs[SignInRequest])

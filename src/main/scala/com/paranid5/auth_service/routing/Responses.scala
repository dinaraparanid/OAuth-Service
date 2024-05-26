package com.paranid5.auth_service.routing

import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason

import org.http4s.Response
import org.http4s.dsl.io.*

private val DefaultRedirect = "http://0.0.0.0:4000/"

private def invalidBody: IO[Response[IO]] =
  BadRequest("Invalid body")

private def wrongEmail: IO[Response[IO]] =
  NotFound("User with provided email was not found")

private def wrongPassword: IO[Response[IO]] =
  NotFound("User with provided email and password was not found")

private def tokenNotFound: IO[Response[IO]] =
  NotFound("Token was not found")

private def tokenExpired: IO[Response[IO]] =
  Forbidden("Token has expired")

private def invalidToken: InvalidTokenReason ⇒ IO[Response[IO]] =
  case InvalidTokenReason.Expired         ⇒ tokenExpired
  case InvalidTokenReason.NotFound        ⇒ tokenNotFound
  case InvalidTokenReason.GenerationError ⇒ tokenGenerationError

private def clientNotFound: IO[Response[IO]] =
  NotFound("Client was not found")

private def appNotFound: IO[Response[IO]] =
  NotFound("App was not found")

private def userAlreadyRegistered: IO[Response[IO]] =
  BadRequest("User with such email is already registered")

// TODO: Specify reason
private def somethingWentWrong: IO[Response[IO]] =
  InternalServerError("Something went wrong")

private def credentialsGenerationError: IO[Response[IO]] =
  InternalServerError("User credentials generation error. Try again")

private def tokenGenerationError: IO[Response[IO]] =
  InternalServerError("Token generation url")

private def redirectToCallbackUrl(callbackUrl: String): IO[Response[IO]] =
  Ok(callbackUrl)

package com.paranid5.auth_service.di

import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.{IOTransactor, getTransactor}

import io.github.cdimascio.dotenv.Dotenv

final class AppModule(val dotenv: Dotenv):
  lazy val transcactor: IOTransactor = getTransactor(dotenv)
  lazy val userModule:  UserModule   = UserModule(transcactor)
  lazy val oauthModule: OAuthModule  = OAuthModule(transcactor)

object AppModule:
  def apply[T](launch: AppModule ⇒ IO[T]): IO[Either[Throwable, T]] =
    launchDotenv((new AppModule(_)) >>> (launch(_)))

  private def launchDotenv[T](f: Dotenv ⇒ IO[T]): IO[Either[Throwable, T]] =
    for
      dotenvRes ← IO(Dotenv.load()).attempt
      res       ← dotenvRes.map(f).sequence
    yield res

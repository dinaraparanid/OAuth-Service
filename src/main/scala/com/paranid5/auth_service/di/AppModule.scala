package com.paranid5.auth_service.di

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.mail.PlatformMailer

import io.github.cdimascio.dotenv.Dotenv

type AppDependencies[F] = Reader[AppModule, F]

final class AppModule(val dotenv: Dotenv):
  lazy val transactor:  IOTransactor   = IOTransactor(dotenv)
  lazy val mailer:      PlatformMailer = PlatformMailer(dotenv)
  lazy val userModule:  UserModule     = UserModule(transactor)
  lazy val oauthModule: OAuthModule    = OAuthModule(transactor)

object AppModule:
  def apply[T](launch: AppModule ⇒ IO[T]): IO[Either[Throwable, T]] =
    launchDotenv((new AppModule(_)) >>> (launch(_)))

  private def launchDotenv[T](f: Dotenv ⇒ IO[T]): IO[Either[Throwable, T]] =
    for
      dotenvRes ← IO(Dotenv.load()).attempt
      res       ← dotenvRes.map(f).sequence
    yield res

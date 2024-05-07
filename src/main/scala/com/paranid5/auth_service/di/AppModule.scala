package com.paranid5.auth_service.di

import cats.effect.{IO, Resource}
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*

import com.paranid5.auth_service.data.{IOTransactor, getTransactor}

import io.github.cdimascio.dotenv.Dotenv

final class AppModule(
  val allocator: Allocator,
  val dotenv:    Dotenv
):
  lazy val transcactor: IOTransactor = getTransactor(dotenv)
  lazy val userModule:  UserModule   = UserModule(transcactor)
  lazy val oauthModule: OAuthModule  = OAuthModule(transcactor)

object AppModule:
  def apply(runtime: IORuntime): Resource[IO, Either[Throwable, AppModule]] =
    Resource.make(launchDotenv(unsafeCreate(runtime) >>> (IO(_)))):
      _.fold(
        fa = x ⇒ IO pure x.printStackTrace(),
        fb = _.allocator.shutdownAll
      )

  private def unsafeCreate(runtime: IORuntime)(dotenv: Dotenv): AppModule =
    new AppModule(Allocator(using runtime), dotenv)

  private def launchDotenv[T](f: Dotenv ⇒ IO[T]): IO[Either[Throwable, T]] =
    for
      dotenvRes ← IO(Dotenv.load()).attempt
      res       ← dotenvRes.map(f).sequence
    yield res

package com.paranid5.auth_service.domain

import cats.effect.kernel.Sync
import cats.effect.std.SecureRandom
import cats.syntax.all.*

import scala.annotation.tailrec

private val TokenSize:  Int = 45
private val SecretSize: Int = 10

private val CodeChars:    String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-"
private val CodeCharsLen: Int    = CodeChars.length

def generateToken[F[_] : Sync](tokenPrefix: String): F[Either[Throwable, String]] =
  for token ← generateCodeImpl(TokenSize)
    yield f"$tokenPrefix${System.currentTimeMillis}$token".encodedToSha.toEither

def generateSecret[F[_] : Sync]: F[Either[Throwable, String]] =
  for secret ← generateCodeImpl(SecretSize)
    yield f"${System.currentTimeMillis}$secret".encodedToSha.toEither

extension (bytes: Array[Byte])
  private def toHex: String =
    bytes map ("%02x" format _) mkString ""

private def generateCodeImpl[F[_] : Sync](codeLength: Int): F[String] =
  @tailrec
  def impl(
    random:   SecureRandom[F],
    res:      F[String] = Sync[F] pure "",
    iterLeft: Int       = codeLength
  ): F[String] =
    iterLeft match
      case 0 ⇒ res
      case _ ⇒ impl(
        random   = random,
        res      = nextChar(random) map (c ⇒ f"$res$c"),
        iterLeft = iterLeft - 1
      )

  def nextChar(random: SecureRandom[F]): F[Char] =
    for charIdx ← random.betweenInt(minInclusive = 0, maxExclusive = codeLength)
      yield CodeChars(charIdx)

  for
    random ← SecureRandom.javaSecuritySecureRandom[F]
    token  ← impl(random)
  yield token

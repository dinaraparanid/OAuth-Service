package com.paranid5.auth_service.domain

import cats.effect.IO
import cats.effect.std.SecureRandom

import scala.annotation.tailrec

private val TokenSize:  Int = 45
private val SecretSize: Int = 10

private val CodeChars:    String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-"
private val CodeCharsLen: Int    = CodeChars.length

def generateToken(tokenPrefix: String): IO[Either[Throwable, String]] =
  for token ← generateCodeImpl(TokenSize)
    yield f"$tokenPrefix${System.currentTimeMillis}$token".encodedToSha.toEither

def generateSecret: IO[Either[Throwable, String]] =
  for clientSecret ← generateCodeImpl(SecretSize)
    yield f"${System.currentTimeMillis}$clientSecret".encodedToSha.toEither

extension (bytes: Array[Byte])
  private def toHex: String =
    bytes map ("%02x" format _) mkString ""

private def generateCodeImpl(codeLength: Int): IO[String] =
  @tailrec
  def impl(
    random:   SecureRandom[IO],
    res:      IO[String] = IO pure "",
    iterLeft: Int        = codeLength
  ): IO[String] =
    iterLeft match
      case 0 ⇒ res
      case _ ⇒ impl(
        random   = random,
        res      = nextChar(random) map (c ⇒ f"$res$c"),
        iterLeft = iterLeft - 1
      )

  def nextChar(random: SecureRandom[IO]): IO[Char] =
    for charIdx ← random.betweenInt(minInclusive = 0, maxExclusive = codeLength)
      yield CodeChars(charIdx)

  for
    random ← SecureRandom.javaSecuritySecureRandom[IO]
    token  ← impl(random)
  yield token

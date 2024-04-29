package com.paranid5.auth_service.token

import cats.effect.IO
import cats.effect.std.SecureRandom

import java.security.MessageDigest
import scala.annotation.tailrec

private val TokenSize:     Int    = 45
private val TokenChars:    String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-"
private val TokenCharsLen: Int    = TokenChars.length
private val GeneratorAlgo: String = "SHA-256"

def generateToken(tokenPrefix: String): IO[Either[Throwable, String]] =
  for
    token  ← generateTokenImpl
    result ← encodeToSha(f"$tokenPrefix${System.currentTimeMillis}$token")
  yield result

extension (bytes: Array[Byte])
  private def toHex: String =
    bytes map ("%02x".format(_)) mkString ""

private def encodeToSha(s: String): IO[Either[Throwable, String]] =
  def impl: String =
    MessageDigest
      .getInstance(GeneratorAlgo)
      .digest(s.getBytes("UTF-8"))
      .toHex

  IO(impl).attempt

private def generateTokenImpl: IO[String] =
  @tailrec
  def impl(
    random:   SecureRandom[IO],
    res:      IO[String] = IO pure "",
    iterLeft: Int        = TokenCharsLen
  ): IO[String] =
    if iterLeft == 0 then
      return res

    impl(
      random   = random,
      res      = nextTokenChar(random) map (c ⇒ f"$res$c"),
      iterLeft = iterLeft - 1
    )

  def nextTokenChar(random: SecureRandom[IO]): IO[Char] =
    for charIdx ← random.betweenInt(minInclusive = 0, maxExclusive = TokenCharsLen)
      yield TokenChars(charIdx)

  for
    random ← SecureRandom.javaSecuritySecureRandom[IO]
    token  ← impl(random)
  yield token

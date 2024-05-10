package com.paranid5.auth_service

import cats.syntax.all.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec

import com.paranid5.auth_service.domain.generateToken

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

private val TokenPattern = "[0-9A-Za-z_.-]{64}".r

class TokenGenTest extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  private def isTokenResMatches(tokenRes: Either[Throwable, String]): Boolean =
    tokenRes map TokenPattern.matches getOrElse false

  "Token Generation" - {
    "works" in {
      val t1 = generateToken("biba")
      val t2 = generateToken("aboba")
      val t3 = generateToken("SUS")
      val t4 = generateToken("AMOGUS")

      (t1 :: t2 :: t3 :: t4 :: Nil)
        .sequence
        .map { list ⇒ assert(list forall isTokenResMatches) }
    }
  }


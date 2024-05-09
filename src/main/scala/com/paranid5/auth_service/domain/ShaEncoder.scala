package com.paranid5.auth_service.domain

import java.security.MessageDigest
import scala.util.Try

private val GeneratorAlgo: String = "SHA-256"

extension (s: String)
  def encodedToSha: Try[String] =
    Try:
      MessageDigest
        .getInstance(GeneratorAlgo)
        .digest(s `getBytes` "UTF-8")
        .toHex

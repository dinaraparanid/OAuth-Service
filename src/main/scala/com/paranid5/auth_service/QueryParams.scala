package com.paranid5.auth_service

import org.http4s.dsl.io.*

object ClientIdParamMatcher     extends QueryParamDecoderMatcher[Int]("client_id")
object ClientSecretParamMatcher extends QueryParamDecoderMatcher[String]("client_secret")
object RedirectUrlParamMatcher  extends QueryParamDecoderMatcher[String]("redirect_url")
object GrantTypeParamMatcher    extends QueryParamDecoderMatcher[String]("grant_type")
object AuthCodeParamMatcher     extends QueryParamDecoderMatcher[String]("auth_code")

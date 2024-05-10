package com.paranid5.auth_service.routing

import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

private object ClientIdParamMatcher     extends QueryParamDecoderMatcher[Long]("client_id")
private object ClientSecretParamMatcher extends QueryParamDecoderMatcher[String]("client_secret")
private object AppIdParamMatcher        extends QueryParamDecoderMatcher[Long]("app_id")
private object AppSecretParamMatcher    extends QueryParamDecoderMatcher[String]("app_secret")
private object AppNameParamMatcher      extends QueryParamDecoderMatcher[String]("app_name")
private object AppThumbnailParamMatcher extends OptionalQueryParamDecoderMatcher[String]("app_thumbnail")
private object RedirectUrlParamMatcher  extends OptionalQueryParamDecoderMatcher[String]("redirect_url")
private object AuthCodeParamMatcher     extends QueryParamDecoderMatcher[String]("auth_code")
private object AccessTokenParamMatcher  extends QueryParamDecoderMatcher[String]("access_token")

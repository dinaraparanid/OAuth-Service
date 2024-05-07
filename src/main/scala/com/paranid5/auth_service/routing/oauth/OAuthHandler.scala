package com.paranid5.auth_service.routing.oauth

import com.paranid5.auth_service.data.user.entity.User
import scalaoauth2.provider.{AccessToken, AuthInfo, AuthorizationRequest, ClientCredential, DataHandler}

import scala.concurrent.Future

final class OAuthHandler extends DataHandler[User]:
  override def validateClient(
    maybeCredential: Option[ClientCredential],
    request:         AuthorizationRequest
  ): Future[Boolean] = ???

  override def findUser(
    maybeCredential: Option[ClientCredential],
    request:         AuthorizationRequest
  ): Future[Option[User]] = ???

  override def createAccessToken(
    authInfo: AuthInfo[User]
  ): Future[AccessToken] = ???

  override def getStoredAccessToken(
    authInfo: AuthInfo[User]
  ): Future[Option[AccessToken]] = ???

  override def refreshAccessToken(
    authInfo: AuthInfo[User],
    refreshToken: String
  ): Future[AccessToken] = ???

  override def findAuthInfoByCode(
    code: String
  ): Future[Option[AuthInfo[User]]] = ???

  override def deleteAuthCode(code: String): Future[Unit] = ???

  override def findAuthInfoByRefreshToken(
    refreshToken: String
  ): Future[Option[AuthInfo[User]]] = ???

  override def findAuthInfoByAccessToken(
    accessToken: AccessToken
  ): Future[Option[AuthInfo[User]]] = ???

  override def findAccessToken(
    token: String
  ): Future[Option[AccessToken]] = ???


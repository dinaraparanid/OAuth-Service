package com.paranid5.auth_service.data.oauth.client

import cats.Applicative
import cats.syntax.all.*

import com.paranid5.auth_service.data.oauth.client.entity.AppEntity

trait AppDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def createTable(): F[Unit]

    def getClientApps(clientId: Long): F[List[AppEntity]]

    def getApp(
      appId:     Long,
      appSecret: String
    ): F[Option[AppEntity]]

    def insertApp(
      appSecret:    String,
      appName:      String,
      appThumbnail: Option[String],
      callbackUrl:  Option[String],
      clientId:     Long,
    ): F[Long]

    def deleteApp(appId: Long): F[Unit]

    def deleteClientApps(clientId: Long): F[Unit]

    def updateApp(
      appId:           Long,
      newAppName:      String,
      newAppThumbnail: Option[String],
      newCallbackUrl:  Option[String],
    ): F[Unit]

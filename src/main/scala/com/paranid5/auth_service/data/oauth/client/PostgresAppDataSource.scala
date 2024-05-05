package com.paranid5.auth_service.data.oauth.client

import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.ops.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresAppDataSource

object PostgresAppDataSource:
  given AppDataSource[ConnectionIO, PostgresAppDataSource] with
    extension (source: PostgresAppDataSource)
      override infix def getClientApps(clientId: Long): ConnectionIO[List[AppEntity]] =
        sql"""SELECT * FROM "App" WHERE client_id = $clientId""".list[AppEntity]

      override infix def getApp(
        appId:     Long,
        appSecret: String
      ): ConnectionIO[Option[AppEntity]] =
        sql"""
        SELECT * FROM "App"
        WHERE app_id = $appId AND app_secret = $appSecret
        """.option[AppEntity]

      override infix def insertApp(
        appId:        Long,
        appSecret:    String,
        appName:      String,
        appThumbnail: Option[String],
        callbackUrl:  Option[String],
        clientId:     Long,
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "App" (app_id, app_secret, app_name, thumbnail, callback_url, client_id)
        VALUES ($appId, $appSecret, $appName, $appThumbnail, $callbackUrl, $clientId)
        """.effect

      override infix def deleteApp(appId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM "App" WHERE app_id = $appId""".effect

      override infix def deleteClientApps(clientId: Long): ConnectionIO[Unit] =
        sql""" DELETE FROM "App" WHERE client_id = $clientId""".effect

      override def updateApp(
        appId:           Long,
        newAppName:      String,
        newAppThumbnail: Option[String],
        newCallbackUrl:  Option[String],
      ): ConnectionIO[Unit] =
        sql"""
        UPDATE "App"
        SET app_name = $newAppName,
            thumbnail = $newAppThumbnail,
            callback_url = $newCallbackUrl
        WHERE app_id = $appId
        """.effect

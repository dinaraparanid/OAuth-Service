package com.paranid5.auth_service.data.oauth.client

import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.ops.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresAppDataSource

object PostgresAppDataSource:
  given AppDataSource[ConnectionIO, PostgresAppDataSource] with
    extension (source: PostgresAppDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "App" (
          app_id SERIAL PRIMARY KEY,
          app_secret TEXT NOT NULL UNIQUE,
          app_name TEXT NOT NULL CHECK (app_name <> ''),
          thumbnail TEXT,
          callback_url TEXT,
          client_id INTEGER NOT NULL REFERENCES "Client"(client_id) ON DELETE CASCADE
        )
        """.effect

      override def getClientApps(clientId: Long): ConnectionIO[List[AppEntity]] =
        sql"""SELECT * FROM "App" WHERE client_id = $clientId""".list[AppEntity]

      override def getApp(
        appId:     Long,
        appSecret: String
      ): ConnectionIO[Option[AppEntity]] =
        sql"""
        SELECT * FROM "App"
        WHERE app_id = $appId AND app_secret = $appSecret
        """.option[AppEntity]

      override def insertApp(
        appSecret:    String,
        appName:      String,
        appThumbnail: Option[String],
        callbackUrl:  Option[String],
        clientId:     Long,
      ): ConnectionIO[Long] =
        sql"""
        INSERT INTO "App" (app_secret, app_name, thumbnail, callback_url, client_id)
        VALUES ($appSecret, $appName, $appThumbnail, $callbackUrl, $clientId)
        RETURNING app_id
        """.serialId

      override def deleteApp(appId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM "App" WHERE app_id = $appId""".effect

      override def deleteClientApps(clientId: Long): ConnectionIO[Unit] =
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

package com.paranid5.auth_service.data.oauth.client

import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.ops.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresClientDataSource

object PostgresClientDataSource:
  given ClientDataSource[ConnectionIO, PostgresClientDataSource] with
    extension (source: PostgresClientDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "Client" (
          client_id SERIAL PRIMARY KEY REFERENCES "User"(user_id) ON DELETE CASCADE,
          client_secret TEXT NOT NULL
        )
        """.effect

      override def clients: ConnectionIO[List[ClientEntity]] =
        sql"""SELECT * FROM "Client"""".list[ClientEntity]

      override def getClient(clientId: Long): ConnectionIO[Option[ClientEntity]] =
        sql"""
        SELECT * FROM  "Client"
        WHERE client_id = $clientId
        """.option[ClientEntity]

      override def findClient(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Option[ClientEntity]] =
        sql"""
        SELECT * FROM  "Client"
        WHERE client_id = $clientId AND client_secret = $clientSecret
        """.option[ClientEntity]

      override def insertClient(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "Client" (client_id, client_secret)
        VALUES ($clientId, $clientSecret)
        """.effect

      override def deleteClient(clientId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM  "Client" WHERE client_id = $clientId""".effect

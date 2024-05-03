package com.paranid5.auth_service.data.oauth.client

import cats.effect.IO
import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresClientDataSource

object PostgresClientDataSource:
  given ClientDataSource[ConnectionIO, PostgresClientDataSource] with
    extension (source: PostgresClientDataSource)
      override def clients: ConnectionIO[List[ClientEntity]] =
        sql"""SELECT * FROM "Client"""".query[ClientEntity].to[List]

      override def isClientExits(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Boolean] =
        sql"""
        SELECT EXISTS(
          SELECT 1
          FROM  "Client"
          WHERE client_id = $clientId AND client_secret = $clientSecret
        )
        """.query[Boolean].unique

      override def storeClient(
        clientId:     Long,
        clientSecret: String
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "Client" (client_id, client_secret)
        VALUES ($clientId, $clientSecret)
        """.update.run.map(_ ⇒ ())

      override def deleteClient(clientId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM  "Client" WHERE client_id = $clientId""".update.run.map(_ ⇒ ())


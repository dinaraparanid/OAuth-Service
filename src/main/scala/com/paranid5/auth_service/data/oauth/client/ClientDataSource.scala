package com.paranid5.auth_service.data.oauth.client

import cats.Applicative
import cats.syntax.all.*
import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity

trait ClientDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def createTable(): F[Unit]

    def clients: F[List[ClientEntity]]

    def getClient(
      clientId:     Long,
      clientSecret: String
    ): F[Option[ClientEntity]]

    def insertClient(
      clientId:     Long,
      clientSecret: String
    ): F[Unit]

    def insertClient(client: ClientEntity): F[Unit] =
      source.insertClient(
        clientId     = client.clientId,
        clientSecret = client.clientSecret
      )

    def insertClients(clients: List[ClientEntity]): F[Unit] =
      clients
        .map(source.insertClient)
        .sequence
        .map(_ ⇒ ())

    def deleteClient(clientId: Long): F[Unit]


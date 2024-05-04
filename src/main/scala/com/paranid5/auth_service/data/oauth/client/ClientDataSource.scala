package com.paranid5.auth_service.data.oauth.client

import cats.Applicative
import cats.syntax.all.*
import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity

trait ClientDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def clients: F[List[ClientEntity]]

    def getClient(
      clientId:     Long,
      clientSecret: String
    ): F[Option[ClientEntity]]

    def storeClient(
      clientId:     Long,
      clientSecret: String
    ): F[Unit]

    infix def storeClient(client: ClientEntity): F[Unit] =
      source.storeClient(
        clientId     = client.clientId,
        clientSecret = client.clientSecret
      )

    infix def storeClients(clients: List[ClientEntity]): F[Unit] =
      clients
        .map(source storeClient _)
        .sequence
        .map(_ ⇒ ())

    def deleteClient(clientId: Long): F[Unit]


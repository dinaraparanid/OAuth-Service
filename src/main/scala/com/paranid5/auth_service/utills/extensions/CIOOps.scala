package com.paranid5.auth_service.utills.extensions

import cats.effect.MonadCancelThrow
import cats.syntax.all.*

import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*
import doobie.util.transactor.Transactor

extension[T, M[_] : MonadCancelThrow] (effect: ConnectionIO[M[T]])
  infix def flatTransact(transactor: Transactor[M]): M[T] =
    effect.transact(transactor).flatten

package com.paranid5.auth_service.data.ops

import cats.syntax.all.*

import doobie.Read
import doobie.free.connection.ConnectionIO
import doobie.util.fragment.Fragment

extension (query: Fragment)
  def list[B: Read]: ConnectionIO[List[B]] =
    query.query[B].to[List].attempt.map:
      _.fold(
        fa = e ⇒ {
          e.printStackTrace()
          Nil
        },
        fb = identity
      )

  def option[B: Read]: ConnectionIO[Option[B]] =
    query.list[B].map(_.headOption)

  def effect: ConnectionIO[Unit] =
    query.update.run.attempt.map:
      _.fold(
        fa = _.printStackTrace(),
        fb = _ ⇒ ()
      )

  def serialId: ConnectionIO[Long] =
    query.query[Long].unique

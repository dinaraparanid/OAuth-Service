package com.paranid5.auth_service.data.ops

import doobie.Read
import doobie.free.connection.ConnectionIO
import doobie.util.fragment.Fragment

extension (query: Fragment)
  def list[B: Read]: ConnectionIO[List[B]] =
    query.query[B].to[List]

  def option[B: Read]: ConnectionIO[Option[B]] =
    query.list[B].map(_.headOption)

  def effect: ConnectionIO[Unit] =
    query.update.run.map(_ ⇒ ())

  def serialId: ConnectionIO[Long] =
    query.query[Long].unique

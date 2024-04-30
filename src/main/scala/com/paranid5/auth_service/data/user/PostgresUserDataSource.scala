package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.user.entity.User
import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresUserDataSource

object PostgresUserDataSource:
  given UserDataSource[ConnectionIO, PostgresUserDataSource] with
    extension (source: PostgresUserDataSource)
      override def users: ConnectionIO[List[User]] =
        sql"""SELECT * FROM "User""""
          .query[User]
          .to[List]

      override def storeUser(
        userId:          Long,
        username:        String,
        email:           String,
        encodedPassword: String
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "User" (user_id, username, email, password)
        VALUES ($userId, $username, $email, $encodedPassword)
        """.update.run.map(_ ⇒ ())

      override def updateUser(
        userId:             Long,
        newUsername:        String,
        newEmail:           String,
        newEncodedPassword: String
      ): ConnectionIO[Unit] =
        sql"""
        UPDATE "User" SET
          username = $newUsername,
          email = $newEmail,
          password = $newEncodedPassword
        WHERE user_id = $userId
        """.update.run.map(_ ⇒ ())

      override def deleteUser(userId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM "User" WHERE user_id = $userId""".update.run.map(_ ⇒ ())

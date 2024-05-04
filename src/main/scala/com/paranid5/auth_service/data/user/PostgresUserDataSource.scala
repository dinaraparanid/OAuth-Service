package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.data.ops.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresUserDataSource

object PostgresUserDataSource:
  given UserDataSource[ConnectionIO, PostgresUserDataSource] with
    extension (source: PostgresUserDataSource)
      override def users: ConnectionIO[List[User]] =
        sql"""SELECT * FROM "User"""".list[User]

      override def storeUser(
        userId:          Long,
        username:        String,
        email:           String,
        encodedPassword: String
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "User" (user_id, username, email, password)
        VALUES ($userId, $username, $email, $encodedPassword)
        """.effect

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
        """.effect

      override def deleteUser(userId: Long): ConnectionIO[Unit] =
        sql"""DELETE FROM "User" WHERE user_id = $userId""".effect

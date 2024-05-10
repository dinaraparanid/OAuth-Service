package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.data.ops.*

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresUserDataSource

object PostgresUserDataSource:
  given UserDataSource[ConnectionIO, PostgresUserDataSource] with
    extension (source: PostgresUserDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "User" (
          user_id SERIAL PRIMARY KEY,
          username TEXT NOT NULL,
          email TEXT NOT NULL UNIQUE,
          password TEXT NOT NULL
        )
        """.effect

      override def users: ConnectionIO[List[User]] =
        sql"""SELECT * FROM "User"""".list[User]

      override def getUser(userId: Long): ConnectionIO[Option[User]] =
        sql"""SELECT * FROM "User" WHERE user_id = $userId""".option[User]

      override def getUserByEmail(email: String): ConnectionIO[Option[User]] =
        sql"""SELECT * FROM "User" WHERE email = $email""".option[User]

      override def storeUser(
        username:        String,
        email:           String,
        encodedPassword: String
      ): ConnectionIO[Long] =
        sql"""
        INSERT INTO "User" (username, email, password)
        VALUES ($username, $email, $encodedPassword)
        RETURNING user_id
        """.serialId

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

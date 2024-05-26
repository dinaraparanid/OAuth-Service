package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.ops.*
import com.paranid5.auth_service.data.user.entity.EmailConfirmCode

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

final class PostgresEmailConfirmCodeDataSource

object PostgresEmailConfirmCodeDataSource:
  given EmailConfirmCodeDataSource[ConnectionIO, PostgresEmailConfirmCodeDataSource] with
    extension (source: PostgresEmailConfirmCodeDataSource)
      override def createTable(): ConnectionIO[Unit] =
        sql"""
        CREATE TABLE IF NOT EXISTS "EmailConfirmCode" (
          email TEXT NOT NULL PRIMARY KEY REFERENCES "User"(email) ON DELETE CASCADE,
          confirm_code TEXT NOT NULL UNIQUE
        )
        """.effect

      override def getConfirmationCode(email: String): ConnectionIO[Option[EmailConfirmCode]] =
        sql"""SELECT * FROM "EmailConfirmCode" WHERE email = $email""".option

      override def findConfirmationCode(code: String): ConnectionIO[Option[EmailConfirmCode]] =
        sql"""SELECT * FROM "EmailConfirmCode" WHERE confirm_code = $code""".option

      override def storeConfirmationCode(
        email:           String,
        confirmationCode: String,
      ): ConnectionIO[Unit] =
        sql"""
        INSERT INTO "EmailConfirmCode" (email, confirm_code)
        VALUES ($email, $confirmationCode)
        """.effect

      override def removeConfirmationCodeByEmail(email: String): ConnectionIO[Unit] =
        sql"""DELETE FROM "EmailConfirmCode" WHERE email = $email""".effect

      override def removeConfirmationCode(code: String): ConnectionIO[Unit] =
        sql"""DELETE FROM "EmailConfirmCode" WHERE confirm_code = $code""".effect

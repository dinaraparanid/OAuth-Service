package com.paranid5.auth_service.data.user

import cats.Applicative
import com.paranid5.auth_service.data.user.entity.{EmailConfirmCode, User}

trait UserRepository[F[_] : Applicative, R]:
  extension (repository: R)
    def createTables(): F[Unit]

    def users: F[List[User]]

    def getUser(userId: Long): F[Option[User]]

    def getUserByEmail(email: String): F[Option[User]]

    def storeUser(
      username:        String,
      email:           String,
      encodedPassword: String
    ): F[Long]

    def updateUser(
      userId:             Long,
      newUsername:        String,
      newEmail:           String,
      newEncodedPassword: String
    ): F[Unit]

    def deleteUser(userId: Long): F[Unit]

    def getConfirmationCode(email: String): F[Option[EmailConfirmCode]]

    def findConfirmationCode(code: String): F[Option[EmailConfirmCode]]

    def updateConfirmationCode(
      email:           String,
      confirmationCode: String,
    ): F[Unit]

    def removeConfirmationCode(code: String): F[Unit]

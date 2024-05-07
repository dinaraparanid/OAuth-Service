package com.paranid5.auth_service.data.user

import cats.Applicative
import com.paranid5.auth_service.data.user.entity.User
import io.github.cdimascio.dotenv.Dotenv

trait UserRepository[F[_] : Applicative, R]:
  extension (repository: R)
    def createTables(): F[Unit]

    def users: F[List[User]]

    def getUser(userId: Long): F[Option[User]]

    def storeUser(
      userId:          Long,
      username:        String,
      email:           String,
      encodedPassword: String
    ): F[Unit]

    def updateUser(
      userId:             Long,
      newUsername:        String,
      newEmail:           String,
      newEncodedPassword: String
    ): F[Unit]

    def deleteUser(userId: Long): F[Unit]

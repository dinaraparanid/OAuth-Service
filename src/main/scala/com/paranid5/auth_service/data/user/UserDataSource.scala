package com.paranid5.auth_service.data.user

import cats.Applicative
import cats.syntax.all.*
import com.paranid5.auth_service.data.user.entity.User

trait UserDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def createTable(): F[Unit]

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

    def updateUser(updatedUser: User): F[Unit] =
      source.updateUser(
        userId             = updatedUser.userId,
        newUsername        = updatedUser.username,
        newEmail           = updatedUser.email,
        newEncodedPassword = updatedUser.encodedPassword
      )

    def updateUsers(updatedUsers: List[User]): F[Unit] =
      updatedUsers
        .map(source.updateUser)
        .sequence
        .map(_ ⇒ ())

    def deleteUser(userId: Long): F[Unit]

package com.paranid5.auth_service.data.user

import cats.Applicative
import cats.syntax.all.*
import com.paranid5.auth_service.data.user.entity.User

trait UserDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def users: F[List[User]]

    def storeUser(
      userId:          Long,
      username:        String,
      email:           String,
      encodedPassword: String
    ): F[Unit]

    infix def storeUser(user: User): F[Unit] =
      source.storeUser(
        userId          = user.userId,
        username        = user.username,
        email           = user.email,
        encodedPassword = user.encodedPassword
      )

    infix def storeUsers(users: List[User]): F[Unit] =
      users
        .map(source storeUser _)
        .sequence
        .map(_ ⇒ ())

    def updateUser(
      userId:             Long,
      newUsername:        String,
      newEmail:           String,
      newEncodedPassword: String
    ): F[Unit]

    infix def updateUser(updatedUser: User): F[Unit] =
      source.updateUser(
        userId             = updatedUser.userId,
        newUsername        = updatedUser.username,
        newEmail           = updatedUser.email,
        newEncodedPassword = updatedUser.encodedPassword
      )

    infix def updateUsers(updatedUsers: List[User]): F[Unit] =
      updatedUsers
        .map(source updateUser _)
        .sequence
        .map(_ ⇒ ())

    infix def deleteUser(userId: Long): F[Unit]

package com.paranid5.auth_service.data.user

import cats.effect.IO

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.user.entity.User

import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*

type UserRepositoryCIO[R] = UserRepository[ConnectionIO, R]

trait UserTransactions[R : UserRepositoryCIO]:
  extension (repository: R)
    protected def transactor: IOTransactor

    def createTablesTransact(): IO[Unit] =
      repository
        .createTables()
        .transact(repository.transactor)

    def usersTransact: IO[List[User]] =
      repository
        .users
        .transact(repository.transactor)

    def getUserTransact(userId: Long): IO[Option[User]] =
      repository
        .getUser(userId)
        .transact(repository.transactor)

    def getUserByEmailTransact(email: String): IO[Option[User]] =
      repository
        .getUserByEmail(email)
        .transact(repository.transactor)

    def storeUserTransact(
      username:        String,
      email:           String,
      encodedPassword: String
    ): IO[Long] =
      repository
        .storeUser(username, email, encodedPassword)
        .transact(repository.transactor)

    def updateUserTransact(
      userId:             Long,
      newUsername:        String,
      newEmail:           String,
      newEncodedPassword: String
    ): IO[Unit] =
      repository
        .updateUser(userId, newUsername, newEmail, newEncodedPassword)
        .transact(repository.transactor)

    def deleteUserTransact(userId: Long): IO[Unit] =
      repository
        .deleteUser(userId)
        .transact(repository.transactor)

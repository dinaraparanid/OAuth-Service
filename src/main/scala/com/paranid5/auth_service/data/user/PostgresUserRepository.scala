package com.paranid5.auth_service.data.user

import cats.effect.IO

import com.paranid5.auth_service.data.*
import com.paranid5.auth_service.data.user.entity.User

import doobie.syntax.all.*

final class PostgresUserRepository(
  private val transactor:     IOTransactor,
  private val userDataSource: PostgresUserDataSource
)

object PostgresUserRepository:
  given UserRepository[IO, PostgresUserRepository] with
    extension (repository: PostgresUserRepository)
      override def createTables(): IO[Unit] =
        repository
          .userDataSource
          .createTable()
          .transact(repository.transactor)

      override def users: IO[List[User]] =
        repository
          .userDataSource
          .users
          .transact(repository.transactor)

      override def getUser(userId: Long): IO[Option[User]] =
        repository
          .userDataSource
          .getUser(userId)
          .transact(repository.transactor)

      override def getUserByEmail(email: String): IO[Option[User]] =
        repository
          .userDataSource
          .getUserByEmail(email)
          .transact(repository.transactor)

      override def storeUser(
        username:        String,
        email:           String,
        encodedPassword: String
      ): IO[Long] =
        repository
          .userDataSource
          .storeUser(username, email, encodedPassword)
          .transact(repository.transactor)

      override def updateUser(
        userId:             Long,
        newUsername:        String,
        newEmail:           String,
        newEncodedPassword: String
      ): IO[Unit] =
        repository
          .userDataSource
          .updateUser(userId, newUsername, newEmail, newEncodedPassword)
          .transact(repository.transactor)

      override def deleteUser(userId: Long): IO[Unit] =
        repository
          .userDataSource
          .deleteUser(userId)
          .transact(repository.transactor)

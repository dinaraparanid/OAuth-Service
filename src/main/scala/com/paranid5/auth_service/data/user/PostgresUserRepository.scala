package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.*
import com.paranid5.auth_service.data.user.entity.User

import cats.effect.IO

import doobie.syntax.all.*

import io.github.cdimascio.dotenv.Dotenv

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

      override def storeUser(
        userId:          Long,
        username:        String,
        email:           String,
        encodedPassword: String
      ): IO[Unit] =
        repository
          .userDataSource
          .storeUser(userId, username, email, encodedPassword)
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

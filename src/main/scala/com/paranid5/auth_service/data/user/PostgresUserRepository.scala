package com.paranid5.auth_service.data.user

import com.paranid5.auth_service.data.*
import com.paranid5.auth_service.data.user.entity.User

import doobie.free.connection.ConnectionIO

final class PostgresUserRepository(
  private val transactor:     IOTransactor,
  private val userDataSource: PostgresUserDataSource
)

object PostgresUserRepository:
  given UserRepository[ConnectionIO, PostgresUserRepository] with
    extension (repository: PostgresUserRepository)
      override def createTables(): ConnectionIO[Unit] =
        repository
          .userDataSource
          .createTable()

      override def users: ConnectionIO[List[User]] =
        repository
          .userDataSource
          .users

      override def getUser(userId: Long): ConnectionIO[Option[User]] =
        repository
          .userDataSource
          .getUser(userId)

      override def getUserByEmail(email: String): ConnectionIO[Option[User]] =
        repository
          .userDataSource
          .getUserByEmail(email)

      override def storeUser(
        username:        String,
        email:           String,
        encodedPassword: String
      ): ConnectionIO[Long] =
        repository
          .userDataSource
          .storeUser(username, email, encodedPassword)

      override def updateUser(
        userId:             Long,
        newUsername:        String,
        newEmail:           String,
        newEncodedPassword: String
      ): ConnectionIO[Unit] =
        repository
          .userDataSource
          .updateUser(userId, newUsername, newEmail, newEncodedPassword)

      override def deleteUser(userId: Long): ConnectionIO[Unit] =
        repository
          .userDataSource
          .deleteUser(userId)

  given UserTransactions[PostgresUserRepository] with
    extension (repository: PostgresUserRepository)
      override protected def transactor: IOTransactor =
        repository.transactor

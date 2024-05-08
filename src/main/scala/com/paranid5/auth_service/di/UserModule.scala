package com.paranid5.auth_service.di

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.user.*

final class UserModule(transactor: IOTransactor):
  private lazy val userDataSource: PostgresUserDataSource =
    PostgresUserDataSource()

  lazy val userRepository: PostgresUserRepository =
    PostgresUserRepository(transactor, userDataSource)

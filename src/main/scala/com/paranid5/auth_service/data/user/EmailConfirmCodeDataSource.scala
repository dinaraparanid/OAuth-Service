package com.paranid5.auth_service.data.user

import cats.Applicative
import cats.syntax.all.*

import com.paranid5.auth_service.data.user.entity.EmailConfirmCode

trait EmailConfirmCodeDataSource[F[_] : Applicative, S]:
  extension (source: S)
    def createTable(): F[Unit]

    def getConfirmationCode(email: String): F[Option[EmailConfirmCode]]

    def findConfirmationCode(code: String): F[Option[EmailConfirmCode]]

    def storeConfirmationCode(
      email:           String,
      confirmationCode: String,
    ): F[Unit]

    def removeConfirmationCodeByEmail(email: String): F[Unit]
    
    def removeConfirmationCode(code: String): F[Unit]

    def updateConfirmationCode(
      email:           String,
      confirmationCode: String,
    ): F[Unit] =
      source.removeConfirmationCodeByEmail(email) *>
        source.storeConfirmationCode(email, confirmationCode)

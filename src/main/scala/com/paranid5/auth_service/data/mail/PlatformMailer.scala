package com.paranid5.auth_service.data.mail

import courier.Mailer
import io.github.cdimascio.dotenv.Dotenv
import com.paranid5.auth_service.data.{MailerEmail, MailerPassword}

final case class PlatformMailer(email: String, mailer: Mailer)

object PlatformMailer:
  def apply(dotenv: Dotenv): PlatformMailer =
    val email = dotenv `get` MailerEmail

    val mailer = Mailer(host = "smtp.gmail.com", port = 587)
      .auth(true)
      .as(
        user = email,
        pass = dotenv `get` MailerPassword
      )
      .startTls(true)()

    PlatformMailer(email, mailer)

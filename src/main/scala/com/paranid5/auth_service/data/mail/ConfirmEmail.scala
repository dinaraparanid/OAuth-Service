package com.paranid5.auth_service.data.mail

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.di.AppModule

import courier.Defaults.executionContext
import courier.{Envelope, Multipart}

import javax.mail.internet.InternetAddress
import scala.concurrent.Future

private val Subject: String = "Confirm your email for OAuth platform"

def sendConfirmEmailViaMailer(
  username:        String,
  email:           String,
  confirmationUrl:  String,
  confirmationCode: String,
): Reader[AppModule, IO[Either[Throwable, Unit]]] =
  Reader: appModule ⇒
    val platformMailer = appModule.mailer

    def impl(confirmationLink: String): Future[Unit] =
      platformMailer.mailer:
        Envelope
          .from(InternetAddress(platformMailer.email))
          .to(InternetAddress(email))
          .subject(Subject)
          .content:
            Multipart().html:
              confirmEmail(
                name            = username,
                confirmationLink = confirmationLink
              )

    val confirmationLink = f"$confirmationUrl?code=$confirmationCode"
    IO.fromFuture(IO(impl(confirmationLink))).attempt

private def confirmEmail(name: String, confirmationLink: String): String =
  f"""
     |<!DOCTYPE html>
     |<html lang="en">
     |<head>
     |  <meta charset="UTF-8">
     |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
     |  <title>$Subject</title>
     |  <style>
     |    body {
     |      font-family: Arial, sans-serif;
     |      margin: 0;
     |      padding: 0;
     |      background-color: #f5f5f5;
     |    }
     |    .container {
     |      max-width: 600px;
     |      margin: 64px auto;
     |      padding: 32px;
     |      background-color: #fff;
     |      border-radius: 5px;
     |      box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
     |    }
     |    h1 {
     |      font-size: 24px;
     |      margin-bottom: 24px;
     |    }
     |    p {
     |      line-height: 1.5;
     |    }
     |    .button-container {
     |      text-align: center;
     |    }
     |    .button {
     |      display: inline-block;
     |      padding: 12px 24px;
     |      background-color: #3498db;
     |      color: #fff;
     |      text-decoration: none;
     |      font-weight: bold;
     |      border: none;
     |      border-radius: 8px;
     |      cursor: pointer;
     |    }
     |  </style>
     |</head>
     |<body>
     |  <div class="container">
     |    <h1>Welcome, $name!</h1>
     |    <p>You received this email because you recently connected your account to our platform. To confirm your email address, please tap the button below:</p>
     |
     |    <div class="button-container">
     |      <a href="$confirmationLink" class="button">Confirm Email</a>
     |    </div>
     |
     |    <p>If you did not request to connect your account to our platform, you can safely disregard this email.</p>
     |  </div>
     |</body>
     |</html>
     |""".stripMargin
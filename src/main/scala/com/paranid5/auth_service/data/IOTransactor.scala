package com.paranid5.auth_service.data

import cats.effect.IO

import doobie.util.log.{LogEvent, LogHandler}
import doobie.util.transactor
import doobie.util.transactor.Transactor

import io.github.cdimascio.dotenv.Dotenv

type IOTransactor = transactor.Transactor.Aux[IO, Unit]

private def printSqlLogHandler: LogHandler[IO] =
  (logEvent: LogEvent) ⇒
    IO pure println(f"${logEvent.sql} // ${logEvent.args}")

def getTransactor(dotenv: Dotenv): IOTransactor =
  Transactor.fromDriverManager[IO](
    driver     = "org.postgresql.Driver",
    url        = dotenv `get` PostgresDbUrl,
    user       = dotenv `get` PostgresDbUser,
    password   = dotenv `get` PostgresDbPassword,
    logHandler = Option(printSqlLogHandler)
  )

package com.paranid5.auth_service.data

import cats.effect.IO
import doobie.util.transactor

type IOTransactor = transactor.Transactor.Aux[IO, Unit]

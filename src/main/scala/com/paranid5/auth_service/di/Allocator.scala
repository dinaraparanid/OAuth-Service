package com.paranid5.auth_service.di

import cats.effect.{IO, Ref, Resource}
import cats.effect.unsafe.IORuntime

class Allocator(using runtime: IORuntime):
  private val shutdown: Ref[IO, IO[Unit]] =
    Ref.unsafe(IO.unit)

  def allocate[A](resource: Resource[IO, A]): A =
    resource
      .allocated
      .flatMap:
        case (a, release) ⇒ shutdown
          .update(release *> _)
          .map(_ ⇒ a)
      .unsafeRunSync()

  def shutdownAll: IO[Unit] =
    shutdown.getAndSet(IO.unit).flatten

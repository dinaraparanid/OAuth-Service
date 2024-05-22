package com.paranid5.auth_service.utills.extensions

import cats.Applicative
import cats.syntax.all.*

extension[T, R, F1[_] : Applicative, F2[_]: Applicative] (option: Option[T])
  def unwrapSequencedR(ifEmpty: ⇒ F2[R])(f: T ⇒ F1[F2[R]]): F1[F2[R]] =
    option
      .toRight(())
      .map(f)
      .sequence
      .map(_ getOrElse ifEmpty)

  def unwrapSequencedL(ifEmpty: ⇒ F1[F2[R]])(f: T ⇒ F2[R]): F1[F2[R]] =
    option
      .toLeft(())
      .map(_ ⇒ ifEmpty)
      .sequence
      .map(_.fold(fa = f, fb = identity))

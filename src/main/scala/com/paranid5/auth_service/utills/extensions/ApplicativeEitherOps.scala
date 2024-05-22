package com.paranid5.auth_service.utills.extensions

import cats.Applicative
import cats.syntax.all.*

extension[L, R, T, F1[_] : Applicative, F2[_]: Applicative] (either: Either[L, R])
  def foldSequencedR(fa: L ⇒ F2[T])(fb: R ⇒ F1[F2[T]]): F1[F2[T]] =
    either
      .map(fb)
      .sequence
      .map(_.fold(fa = fa, fb = identity))

  def foldSequencedL(fa: L ⇒ F1[F2[T]])(fb: R ⇒ F2[T]): F1[F2[T]] =
    either.swap.foldSequencedR(fb)(fa)

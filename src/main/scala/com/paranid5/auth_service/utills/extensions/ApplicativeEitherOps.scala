package com.paranid5.auth_service.utills.extensions

import cats.Applicative
import cats.syntax.all.*

object ApplicativeEitherOps:
  extension [L, R, T, F1[_]: Applicative, F2[_]: Applicative](either: Either[L, R])
    def foldTraverseR(fa: L ⇒ F2[T])(fb: R ⇒ F1[F2[T]]): F1[F2[T]] =
      either
        .traverse(fb)
        .map(_.fold(fa = fa, fb = identity))

    def foldTraverseL(fa: L ⇒ F1[F2[T]])(fb: R ⇒ F2[T]): F1[F2[T]] =
      either.swap.foldTraverseR(fb)(fa)

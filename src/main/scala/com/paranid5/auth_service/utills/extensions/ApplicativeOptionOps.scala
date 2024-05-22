package com.paranid5.auth_service.utills.extensions

import cats.Applicative
import cats.syntax.all.*

object ApplicativeOptionOps:
  extension [T, R, F1[_]: Applicative, F2[_]: Applicative](option: Option[T])
    def foldTraverseR(ifEmpty: ⇒ F2[R])(f: T ⇒ F1[F2[R]]): F1[F2[R]] =
      option
        .traverse(f)
        .map(_ getOrElse ifEmpty)

    def foldTraverseL(ifEmpty: ⇒ F1[F2[R]])(f: T ⇒ F2[R]): F1[F2[R]] =
      option
        .toLeft(())
        .traverse(_ ⇒ ifEmpty)
        .map(_.fold(fa = f, fb = identity))

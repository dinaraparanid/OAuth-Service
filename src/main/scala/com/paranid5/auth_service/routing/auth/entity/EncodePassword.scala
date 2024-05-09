package com.paranid5.auth_service.routing.auth.entity

trait EncodePassword[T]:
  extension (self: T)
    def withEncodedPassword: T
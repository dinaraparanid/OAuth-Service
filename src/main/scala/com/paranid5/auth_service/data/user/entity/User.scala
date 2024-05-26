package com.paranid5.auth_service.data.user.entity

final case class User(
  userId:          Long,
  username:        String,
  email:           String,
  encodedPassword: String,
)

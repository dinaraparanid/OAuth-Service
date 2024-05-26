package com.paranid5.auth_service.data.user.entity

final case class EmailConfirmCode(
  email:      String,
  confirmCode: String,
)

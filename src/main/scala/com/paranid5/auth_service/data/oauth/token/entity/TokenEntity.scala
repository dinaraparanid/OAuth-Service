package com.paranid5.auth_service.data.oauth.token.entity

case class TokenEntity(
  userId:      Long,
  title:       String,
  value:       Long,
  lifeSeconds: Option[Long],
  createdAt:   Long,
)

package com.paranid5.auth_service.data.oauth.token.entity

final case class TokenScopeRelation(
  clientId:         Long,
  accessTokenTitle: String,
  scope:            String
)

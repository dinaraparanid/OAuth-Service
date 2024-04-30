package com.paranid5.auth_service.data.oauth.token.entity

case class AccessToken(
  entity: TokenEntity,
  scopes: List[TokenScope]
)

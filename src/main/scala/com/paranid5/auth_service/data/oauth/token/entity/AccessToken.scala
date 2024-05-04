package com.paranid5.auth_service.data.oauth.token.entity

final case class AccessToken(
  entity: TokenEntity,
  scopes: List[TokenScope]
)

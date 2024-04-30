package com.paranid5.auth_service.data.oauth

import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, RefreshToken}

case class Client(
  entity:       ClientEntity,
  accessTokens: List[AccessToken],
  refreshToken: Option[RefreshToken]
)

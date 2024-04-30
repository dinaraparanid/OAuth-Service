package com.paranid5.auth_service.data.oauth.token

enum AccessTokenCreationFailure:
  case RefreshTokenExpired, WrongRefreshToken, UserNotFound
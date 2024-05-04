package com.paranid5.auth_service.data.oauth.token.error

enum InvalidTokenReason:
  case Expired, NotFound, GenerationError
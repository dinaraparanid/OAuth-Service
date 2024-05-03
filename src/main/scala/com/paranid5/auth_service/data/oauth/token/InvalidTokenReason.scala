package com.paranid5.auth_service.data.oauth.token

enum InvalidTokenReason:
  case Expired, NotFound, GenerationError
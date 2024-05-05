package com.paranid5.auth_service.data.oauth.token.error

enum InvalidTokenReason extends InvalidOAuthReason:
  case Expired, NotFound, GenerationError
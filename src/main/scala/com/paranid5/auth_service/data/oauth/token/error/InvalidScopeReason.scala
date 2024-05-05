package com.paranid5.auth_service.data.oauth.token.error

enum InvalidScopeReason extends InvalidOAuthReason:
  case TokenNotFound, ScopeAlreadyApplied, ScopeNotApplied
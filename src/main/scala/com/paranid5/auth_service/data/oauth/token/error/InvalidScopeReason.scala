package com.paranid5.auth_service.data.oauth.token.error

enum InvalidScopeReason:
  case TokenNotFound, ScopeAlreadyApplied, ScopeNotApplied
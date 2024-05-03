package com.paranid5.auth_service.data.oauth.token.entity

enum TokenStatus(val title: String):
  case Access  extends TokenStatus("access")
  case Refresh extends TokenStatus("refresh")
package com.paranid5.auth_service.data.oauth.client.entity

final case class AppEntity(
  appId:        Long,
  appSecret:    String,
  appName:      String,
  appThumbnail: Option[String],
  callbackUrl:  Option[String],
  clientId:     Long,
)

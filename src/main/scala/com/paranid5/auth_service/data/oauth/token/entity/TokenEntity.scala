package com.paranid5.auth_service.data.oauth.token.entity

import io.circe.{Decoder, Encoder}

private val MillisInSecond = 1000

final case class TokenEntity(
  clientId:    Long,
  appId:       Option[Long],
  value:       String,
  lifeSeconds: Option[Long],
  createdAt:   Long,
  status:      String
)

object TokenEntity:
  given Encoder[TokenEntity] =
    Encoder.forProduct6("client_id", "app_id", "value", "life_seconds", "created_at", "status"): e ⇒
      (e.clientId, e.appId, e.value, e.lifeSeconds, e.createdAt, e.status)

  given Decoder[TokenEntity] =
    Decoder.forProduct6("client_id", "app_id", "value", "life_seconds", "created_at", "status")(TokenEntity.apply)

extension (token: TokenEntity)
  def actualUntil: Option[Long] =
    token.lifeSeconds map (_ * MillisInSecond + token.createdAt)

  def isActual: Boolean =
    def impl(time: Long): Boolean =
      time > System.currentTimeMillis

    token.actualUntil match
      case None        ⇒ true
      case Some(value) ⇒ impl(value)

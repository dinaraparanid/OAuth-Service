package com.paranid5.auth_service.data.oauth.token.entity

private val MillisInSecond = 1000

case class TokenEntity(
  clientId:    Long,
  title:       Option[String],
  value:       String,
  lifeSeconds: Option[Long],
  createdAt:   Long,
  status:      String
)

extension (token: TokenEntity)
  def actualUntil: Option[Long] =
    token.lifeSeconds map (_ * MillisInSecond + token.createdAt)

  def isActual: Boolean =
    def impl(time: Long): Boolean =
      time > System.currentTimeMillis

    token.actualUntil match
      case None        ⇒ true
      case Some(value) ⇒ impl(value)

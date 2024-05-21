package com.paranid5.auth_service.di

import com.paranid5.auth_service.data.IOTransactor
import com.paranid5.auth_service.data.oauth.PostgresOAuthRepository
import com.paranid5.auth_service.data.oauth.client.*
import com.paranid5.auth_service.data.oauth.token.{PostgresTokenDataSource, PostgresTokenScopeDataSource}

final class OAuthModule(transactor: IOTransactor):
  private lazy val clientModule: OAuthClientModule = OAuthClientModule()
  private lazy val tokenModule:  OAuthTokenModule  = OAuthTokenModule()

  lazy val oauthRepository: PostgresOAuthRepository = PostgresOAuthRepository(
    clientDataSource     = clientModule.clientDataSource,
    appDataSource        = clientModule.appDataSource,
    tokenDataSource      = tokenModule.tokenDataSource,
    tokenScopeDataSource = tokenModule.tokenScopeDataSource
  )

private final class OAuthClientModule:
  lazy val appDataSource:    PostgresAppDataSource    = PostgresAppDataSource()
  lazy val clientDataSource: PostgresClientDataSource = PostgresClientDataSource()

private final class OAuthTokenModule:
  lazy val tokenDataSource:      PostgresTokenDataSource      = PostgresTokenDataSource()
  lazy val tokenScopeDataSource: PostgresTokenScopeDataSource = PostgresTokenScopeDataSource()
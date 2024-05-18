# OAuth Service

[![Scala](https://shields.io/badge/scala-3.4.1-red.svg?logo=scala)](https://www.scala-lang.org/)

## **Developer**
[Paranid5](https://github.com/dinaraparanid)

**OAuth Service** is server-side app for a custom OAuth platform
that allows to integrate authorization for user-defined applications

Project utilizes *PostgreSQL* and *Docker Compose* as a deployment mechanism

## **Rooting / Docs**

* Auth
  * [POST /auth/sign_up](src/main/scala/com/paranid5/auth_service/routing/auth/SignUp.scala)
  * [POST /auth/sign_in](src/main/scala/com/paranid5/auth_service/routing/auth/SignIn.scala)
* OAuth
  * [POST /oauth/authorize (in platform)](src/main/scala/com/paranid5/auth_service/routing/oauth/PlatformAuthorize.scala)
  * [POST /oauth/authorize (in user app)](src/main/scala/com/paranid5/auth_service/routing/oauth/AppAuthorize.scala)
  * [POST /oauth/token (for platform)](src/main/scala/com/paranid5/auth_service/routing/oauth/PlatformToken.scala)
  * [POST /oauth/token (for user app)](src/main/scala/com/paranid5/auth_service/routing/oauth/AppToken.scala)
  * [POST /oauth/refresh (for platform)](src/main/scala/com/paranid5/auth_service/routing/oauth/PlatformRefresh.scala)
  * [POST /oauth/refresh (for user app)](src/main/scala/com/paranid5/auth_service/routing/oauth/AppRefresh.scala)
  * [GET /oauth/user](src/main/scala/com/paranid5/auth_service/routing/oauth/FindUser.scala)
* App
  * [POST /app](src/main/scala/com/paranid5/auth_service/routing/app/Create.scala)
  * [GET /app](src/main/scala/com/paranid5/auth_service/routing/app/Find.scala)
  * [GET /app/all](src/main/scala/com/paranid5/auth_service/routing/app/All.scala)
  * [PATCH /app](src/main/scala/com/paranid5/auth_service/routing/app/Update.scala)
  * [DELETE /app](src/main/scala/com/paranid5/auth_service/routing/app/Delete.scala)

## **Stack**

<ul>
    <li>Scala 3</li>
    <li>Cats Effects</li>
    <li>Http4S</li>
    <li>Circle</li>
    <li>Doobie</li>
    <li>Scala Test + Cats Effects Extensions</li>
    <li>Docker</li>
    <li>PostgreSQL</li>
</ul>

## **System Requirements**
**Java 21** is required to build project

## **License**
*GNU Public License V 3.0*
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.1"

lazy val root = (project in file("."))
  .settings(
    name := "AuthBack",
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-effect"                   % "3.5.4",
      "org.http4s"          %% "http4s-dsl"                    % "0.23.26",
      "org.http4s"          %% "http4s-ember-server"           % "0.23.26",
      "org.http4s"          %% "http4s-circe"                  % "0.23.26",
      "io.circe"            %% "circe-generic"                 % "0.14.7",
      "io.circe"            %% "circe-literal"                 % "0.14.7",
      "com.nulab-inc"       %% "scala-oauth2-core"             % "1.6.0",
      "org.tpolecat"        %% "doobie-core"                   % "1.0.0-RC4",
      "org.tpolecat"        %% "doobie-postgres"               % "1.0.0-RC4",
      "io.github.cdimascio"  % "dotenv-kotlin"                  % "6.4.1",
      "org.scalatest"       %% "scalatest"                     % "3.2.18" % Test,
      "org.typelevel"       %% "cats-effect-testing-scalatest" % "1.5.0"  % Test,
    )
  )

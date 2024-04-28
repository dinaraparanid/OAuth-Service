ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.1"

lazy val root = (project in file("."))
  .settings(
    name := "AuthBack",
    idePackagePrefix     :=  Option("com.paranid5.authback"),
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-effect"                   % "3.5.4",
      "org.http4s"          %% "http4s-dsl"                    % "0.23.26",
      "org.http4s"          %% "http4s-ember-server"           % "0.23.26",
      "org.http4s"          %% "http4s-circe"                  % "0.23.26",
      "io.circe"            %% "circe-generic"                 % "0.14.7",
      "io.circe"            %% "circe-literal"                 % "0.14.7",
      "org.scalatest"       %% "scalatest"                     % "3.2.18" % Test,
      "org.typelevel"       %% "cats-effect-testing-scalatest" % "1.5.0"  % Test
    )
  )

FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.2_13_1.9.9_3.4.1 AS build

COPY ./ ./

EXPOSE 8080

RUN sbt test
ENTRYPOINT ["sbt", "run"]
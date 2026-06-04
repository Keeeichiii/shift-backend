# syntax=docker/dockerfile:1.7

# Multi-stage build: Maven compiles the Spring Boot app, the runtime image keeps only the JRE and app files.
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

RUN addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /app/static-images/cars \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar
COPY --from=build --chown=spring:spring /workspace/src/main/resources/static/images/cars /app/static-images/cars

USER spring
EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

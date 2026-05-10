# Многостадийная сборка: Maven → исполняемый JAR (Spring Boot, Java 17).
# Контекст сборки — корень репозитория; см. .dockerignore.
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/shift-backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/src/main/resources/static/images/cars /app/static-images/cars
RUN chown -R spring:spring /app/app.jar /app/static-images
USER spring
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

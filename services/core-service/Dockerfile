# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package && mvn -q test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S govflow && adduser -S govflow -G govflow
COPY --from=build /workspace/target/core-service-*.jar /app/core-service.jar
USER govflow
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/core-service.jar"]

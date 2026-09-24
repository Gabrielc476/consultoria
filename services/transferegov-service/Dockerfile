# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package && mvn -q test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S govflow && adduser -S govflow -G govflow
COPY --from=build /workspace/target/transferegov-service-*.jar /app/transferegov-service.jar
USER govflow
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/transferegov-service.jar"]

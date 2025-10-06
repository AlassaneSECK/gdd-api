# syntax=docker/dockerfile:1

FROM maven:3.9.7-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN sed -i "s/\r$//" mvnw && chmod +x mvnw
# Preload dependencies to leverage Docker layer caching when sources change
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app
COPY --from=builder /build/target/gdd-api-*.jar ./gdd-api.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/gdd-api.jar"]

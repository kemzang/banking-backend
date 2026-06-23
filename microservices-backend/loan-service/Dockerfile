# syntax=docker/dockerfile:1
# Build stage: package the Spring Boot application using Maven from the image.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,id=banking-maven-repository,target=/root/.m2,sharing=locked \
    mvn -B --no-transfer-progress --settings .mvn/settings-docker.xml -Dmaven.wagon.http.retryHandler.count=5 -DskipTests package

# Runtime stage: execute the built JAR.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]

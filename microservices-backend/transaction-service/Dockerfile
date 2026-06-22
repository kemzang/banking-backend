# Build stage: package the Spring Boot application using Maven wrapper.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw && ./mvnw -DskipTests package

# Runtime stage: execute the built JAR.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

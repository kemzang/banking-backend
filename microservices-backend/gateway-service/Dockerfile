# ---- Etape 1 : build avec Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache des dependances : on copie d'abord le pom
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B dependency:go-offline
# Puis le code source
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Etape 2 : image d'execution legere ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

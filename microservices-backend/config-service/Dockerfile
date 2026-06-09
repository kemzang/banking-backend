# Image d'execution. Le .jar est construit AU PREALABLE sur la machine
# (./mvnw -DskipTests package) puis copie ici.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

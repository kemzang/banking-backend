# Image d'execution. Le .jar est construit AU PREALABLE sur la machine
# (./mvnw -DskipTests package) puis copie ici — pas de build Maven dans Docker.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

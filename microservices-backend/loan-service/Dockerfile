# Image d'execution. Le .jar est construit AU PREALABLE sur la machine
# (./mvnw -DskipTests package) puis copie ici — aucun telechargement Maven
# n'a lieu pendant le build Docker (evite les soucis de reseau/DNS).
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]

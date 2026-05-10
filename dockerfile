FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/lumen-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
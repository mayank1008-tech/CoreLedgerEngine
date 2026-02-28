# We use Eclipse Temurin (a reliable OpenJDK build) instead of the deprecated 'openjdk' image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/LedgerSystem-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

# Multi-stage build: compile with Maven, run on a slim JRE.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Cache dependencies first for faster incremental builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Multi-arch (amd64 + arm64/Apple Silicon). Temurin's -jre-alpine has no arm64 manifest.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/impact-budget-*.jar app.jar
EXPOSE 8080
# Container-aware JVM defaults; tune as needed.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

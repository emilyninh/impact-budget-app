# Multi-stage build: bundle the React SPA into the Spring Boot app, run on a slim JRE.
# The app then serves the UI and the API from one origin (no CORS, one deployable).

# Stage 1 — build the React SPA.
FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2 — build the Spring Boot app, copying the built SPA into static resources so it's
# packaged into the jar (served from classpath:/static).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Cache dependencies first for faster incremental builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -q -B clean package -DskipTests

# Stage 3 — runtime. Multi-arch (amd64 + arm64/Apple Silicon); Temurin's -jre-alpine has no
# arm64 manifest.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/impact-budget-*.jar app.jar
EXPOSE 8080
# Container-aware JVM defaults; tune as needed.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

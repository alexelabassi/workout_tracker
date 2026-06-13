# Build context is the PROJECT ROOT (see docker-compose.yml), so this Dockerfile
# can access both backend/ and frontend/. The Maven build triggers the Vite
# frontend build (frontend-maven-plugin) and bundles it into the Spring Boot jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Warm the Maven dependency cache first for better layer reuse.
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -q -B dependency:go-offline || true

COPY backend backend
COPY frontend frontend
RUN mvn -f backend/pom.xml -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/backend/target/workout.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

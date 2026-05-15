# Multi-stage build for Android Toolkit Server
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :backend:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/build/libs/*.jar app.jar

EXPOSE 8080

# Configure via environment variables
ENV SPRING_PROFILES_ACTIVE=saas
ENV DATABASE_URL=jdbc:postgresql://localhost:5432/androidtoolkit
ENV STORAGE_ENDPOINT=http://localhost:9000
ENV STORAGE_BUCKET=androidtoolkit
ENV JWT_SECRET=change-me-in-production
ENV CORS_ALLOWED_ORIGINS=*

ENTRYPOINT ["java", "-jar", "app.jar"]

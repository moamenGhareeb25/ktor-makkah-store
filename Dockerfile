# Use OpenJDK 17 as the base image
FROM openjdk:17-jdk-slim as builder

# Set the working directory
WORKDIR /app

# Copy the build files
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src

# Build the application
RUN ./gradlew shadowJar

# Final stage
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/app.jar .

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ktor_app
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/ktor_dkwc
      DATABASE_USER: ktor_dkwc_user
      DATABASE_PASSWORD: xPnzorWy9NzjuPeSYSOFOh7fsiSQ9q7f
      FIREBASE_SERVICE_ACCOUNT_PATH: /app/firebase-key.json
    volumes:
      - ./firebase-key.json:/app/firebase-key.json
    depends_on:
      - db
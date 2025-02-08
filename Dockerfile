# Builder Stage
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copy Gradle wrapper and project files
COPY gradlew gradle/ build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Grant execute permissions and build the application
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar --no-daemon

# Final Stage
FROM eclipse-temurin:17-jdk-jammy
# Builder Stage
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copy Gradle files and source code
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Grant execute permissions and build the application
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar --no-daemon

# Final Stage
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set environment variables
ENV FIREBASE_CONFIG=/app/firebase-key.json

# Expose application port
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]


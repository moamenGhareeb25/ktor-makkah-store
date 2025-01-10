# Builder Stage: Use OpenJDK 17 as the base image for building the application
FROM openjdk:17-jdk-slim as builder

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy the source code
COPY src ./src

# Grant execute permissions to the Gradle wrapper
RUN chmod +x ./gradlew

# Build the application with the shadowJar task
RUN ./gradlew shadowJar --no-daemon

# Final Stage: Use OpenJDK 17 as the base image for running the application
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]

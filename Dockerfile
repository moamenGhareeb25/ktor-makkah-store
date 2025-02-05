# Builder Stage
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew shadowJar --no-daemon

# Final Stage
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# Ensure Firebase Config is set as an environment variable
ENV FIREBASE_CONFIG=${FIREBASE_CONFIG}

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

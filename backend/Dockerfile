# Multi-stage Docker build for Trinity College Scheduler Backend

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle/

# Copy source code
COPY src src/

# Add execute permission to the gradlew script
RUN chmod +x gradlew
RUN gradle wrapper

# Build the application
RUN ./gradlew clean bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create data directory for SQLite
RUN mkdir -p /app/database

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy SQLite database (will be overridden by volume mount in production)
COPY /database/trinity_college.sqlite /app/database/trinity_college.sqlite

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

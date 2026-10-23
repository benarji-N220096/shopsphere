# ==============================================================================
# ShopSphere Order Service - Multi-Stage Production Dockerfile
# Base: Eclipse Temurin OpenJDK 21 on Alpine Linux (minimal, secure, non-root)
# ==============================================================================

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy build wrappers and POM
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Normalize line endings and set permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies offline layer
RUN ./mvnw dependency:go-offline -B || true

# Copy source and build executable JAR
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Production Runtime stage
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="ShopSphere DevOps Team"
LABEL description="ShopSphere Order Processing & SAGA Orchestrator Service"

WORKDIR /app

# Create unprivileged application user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from build stage
COPY --from=builder /workspace/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8084

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

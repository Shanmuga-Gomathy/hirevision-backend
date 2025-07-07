# ======== Stage 1: Build the app ========
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy all files
COPY . .

# Build the JAR (skip tests to make build faster)
RUN mvn clean package -DskipTests


# ======== Stage 2: Run the app ========
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the JAR

CMD ["java", "-jar", "/app/JobReferralApp.jar"]


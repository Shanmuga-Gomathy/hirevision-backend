# ======== Stage 1: Build the app ========
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy all files
COPY . .

# Build the JAR (skip tests)
RUN mvn clean package -DskipTests

# ======== Stage 2: Run the app ========
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the built JAR file from builder stage and rename it to app.jar
COPY --from=builder /app/target/JobReferralApp-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "/app/app.jar"]

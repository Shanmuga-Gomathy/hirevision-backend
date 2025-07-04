# Use Java 21 LTS base image
FROM eclipse-temurin:21-jdk

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper files first (to cache dependencies if code changes later)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# ✅ Fix: Add execute permission inside Docker image
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY . .

# Build the application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests

# Expose port used by Spring Boot
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/JobReferralApp-0.0.1-SNAPSHOT.jar"]

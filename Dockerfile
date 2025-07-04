# Use official OpenJDK as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and config files first (to use cache for dependencies)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# ✅ Make mvnw executable
RUN chmod +x mvnw

# Download dependencies for layer caching
RUN ./mvnw dependency:go-offline

# Copy the full source code
COPY . .

# ✅ Ensure mvnw remains executable after copy (some OSes reset permissions)
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose default Spring Boot port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/hirevision-backend-0.0.1-SNAPSHOT.jar"]

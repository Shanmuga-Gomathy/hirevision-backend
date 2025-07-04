# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# ✅ Add this line to fix the permission error
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the project files
COPY . .

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose the Spring Boot default port
EXPOSE 8080

# Replace with your actual JAR name if needed
CMD ["java", "-jar", "target/hirevision-backend-0.0.1-SNAPSHOT.jar"]

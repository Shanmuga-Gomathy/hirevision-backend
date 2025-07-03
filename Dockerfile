# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper and the pom.xml
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (layer caching)
RUN ./mvnw dependency:go-offline

# Copy the rest of the project files
COPY . .

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/*.jar"]

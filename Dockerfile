# Stage 1: Build the app using Maven
FROM openjdk:17-jdk-slim AS build

# Set the working directory inside the container
WORKDIR /app

# Copy Maven files first (for better caching)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the full source code
COPY . .

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the secret application.properties from Render's secrets directory at runtime
# and run the app with the external config
CMD cp /etc/secrets/application.properties ./application.properties && \
    java -jar app.jar --spring.config.location=classpath:/,file:./application.properties

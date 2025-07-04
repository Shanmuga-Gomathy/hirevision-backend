FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy project files
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copy rest of the app
COPY . .

# Build the app
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/JobReferralApp-0.0.1-SNAPSHOT.jar"]

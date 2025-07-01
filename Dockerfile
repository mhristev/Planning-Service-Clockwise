# Use official OpenJDK image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy compiled JAR file
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8083

# Run the application with Docker profile
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "/app/app.jar"]




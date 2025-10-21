# Use a Maven image to build the application
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the entire project to the container
COPY . .

# Build the project
RUN mvn clean package -DskipTests

# Use a slim Java image to run the application
FROM amazoncorretto:21

# Set the working directory for the runtime
WORKDIR /app

# Copy the built JAR file from the Maven build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV PORT 8080

# Run the application
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]

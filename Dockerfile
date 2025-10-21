# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw
# copy sources last to leverage layer cache
COPY src src

# IMPORTANT: skip tests so the build doesn't try to connect to DB/Supabase
RUN ./mvnw -B -DskipTests -DskipITs clean package
# If you use spring-boot:repackage, also add -DskipTests there

# Runtime stage (distroless or slim base is fine)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Cloud Run sets PORT; Spring must listen on it
ENV PORT=8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

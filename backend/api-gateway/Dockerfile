FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S n11 && adduser -S n11 -G n11
USER n11:n11

COPY target/*.jar app.jar
EXPOSE 8000

# Run with JVM memory optimizations
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=50.0", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

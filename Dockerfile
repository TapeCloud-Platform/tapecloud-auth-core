# TapeCloud Auth Core — imagen de producción (la usa Railway).
# Build multi-stage: compila con Maven y corre solo el JAR en un JRE liviano.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /workspace

COPY --from=builder /workspace/target/*.jar app.jar

# Railway inyecta $PORT dinámicamente; la app lo lee vía server.port=${PORT:8080}.
# Healthcheck de Railway: /api/health
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

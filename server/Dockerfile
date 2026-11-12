# syntax=docker/dockerfile:1

# ── Build ────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código fuente
COPY src ./src
RUN mvn -B -q -DskipTests package

# ── Runtime ──────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/target/platform-*.jar /app/app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

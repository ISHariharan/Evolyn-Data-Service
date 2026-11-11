# ── Stage 1: build the jar with Maven ─────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first, in their own layer: when only source code changes,
# Docker reuses this layer and the rebuild is much faster.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src src
RUN mvn -B -q -DskipTests package

# ── Stage 2: a small image with just the Java runtime and the jar ─────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render's free instances have 512 MB of memory:
#  MaxRAMPercentage  - let the Java heap use at most 70% of the container's memory
#  UseSerialGC       - the simplest garbage collector, with the least memory overhead
#  Xss512k           - smaller thread stacks
#  TieredStopAtLevel - faster start-up (matters when the service wakes from sleep)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"

# Render sets PORT; the app reads it (server.port=${PORT:3002})
EXPOSE 3002
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# syntax=docker/dockerfile:1.7

# STAGE 1 — BUILD
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts lombok.config ./
COPY gradle gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies || true

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test

# STAGE 2 — RUNTIME
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --shell /usr/sbin/nologin appuser

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

USER 10001

EXPOSE 8380

HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=40s \
  CMD curl -fsS http://localhost:8380/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=UTC", \
  "-jar", "/app/app.jar"]

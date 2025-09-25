# ---------- Build stage ----------
FROM gradle:8.5-jdk21 AS build

WORKDIR /home/gradle/project

COPY build.gradle* settings.gradle* gradle/ ./

# Download dependencies (cached)
RUN gradle assemble -x test --no-daemon

COPY src/ src/

# Compile with dependencies
RUN gradle build -x test --no-daemon && \
    rm -rf ~/.gradle /home/gradle/.gradle /home/gradle/project/.gradle

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create no‑root user
RUN addgroup -S app && adduser -S -G app appuser

WORKDIR /app

ARG JAR_PATTERN=*.jar
ARG TARGET_JAR=app.jar

COPY --from=build /home/gradle/project/build/libs/${JAR_PATTERN} /app/${TARGET_JAR}

EXPOSE 8080
USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]

HEALTHCHECK --start-period=5s --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8080/api/health || exit 1
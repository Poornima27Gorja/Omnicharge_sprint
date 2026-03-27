# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q && \
    mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="OmniCharge Team <support@omnicharge.com>"
LABEL service="eureka-server"

WORKDIR /app

RUN addgroup -S omnicharge && adduser -S omnicharge -G omnicharge

COPY --from=builder /app/target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

RUN chown omnicharge:omnicharge app.jar

USER omnicharge

EXPOSE 8761

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

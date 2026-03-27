# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

RUN apk add --no-cache curl && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz \
    | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

COPY pom.xml .
COPY src ./src

RUN mvn dependency:go-offline -q && \
    mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="OmniCharge Team <support@omnicharge.com>"
LABEL service="user-service"

WORKDIR /app

RUN addgroup -S omnicharge && adduser -S omnicharge -G omnicharge

COPY --from=builder /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/logs && chown -R omnicharge:omnicharge /app/logs app.jar

USER omnicharge

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
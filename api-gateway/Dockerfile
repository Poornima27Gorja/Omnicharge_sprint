FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn dependency:go-offline -q
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="OmniCharge Team <support@omnicharge.com>"
LABEL service="api-gateway"

WORKDIR /app

RUN addgroup -S omnicharge && adduser -S omnicharge -G omnicharge

COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/logs && chown -R omnicharge:omnicharge /app/logs app.jar

USER omnicharge

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/quarkus-app/ ./
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s CMD curl -fsS http://localhost:8080/q/health/live || exit 1
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

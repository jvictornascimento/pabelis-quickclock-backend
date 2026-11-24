FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system quickclock \
    && useradd --system --gid quickclock --home-dir /app quickclock
WORKDIR /app
COPY --from=build --chown=quickclock:quickclock /workspace/target/quickclock-backend-0.0.1-SNAPSHOT.jar app.jar
USER quickclock
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=15 CMD curl --fail --silent --max-time 3 http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

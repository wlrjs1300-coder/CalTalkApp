FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts build.gradle.kts ./
COPY src src
RUN chmod +x gradlew && ./gradlew clean compileTestJava bootJar --no-daemon --console=plain

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S caltalk && adduser -S -G caltalk caltalk
WORKDIR /app
COPY --from=build --chown=caltalk:caltalk /workspace/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

USER caltalk
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

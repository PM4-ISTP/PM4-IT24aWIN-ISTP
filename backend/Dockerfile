# Stage 1 – build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
# Copy dependency declarations first for better layer caching
RUN ./gradlew dependencies --no-daemon || true
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2 – runtime (JRE only, not JDK)
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

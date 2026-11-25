# Build the jar first: ./mvnw package
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 gateway
WORKDIR /app
COPY target/agent-ready-legacy-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

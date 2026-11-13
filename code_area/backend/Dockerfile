# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S ddb && adduser -S ddb -G ddb
COPY --from=build /src/target/ddb-backend-*.jar app.jar
USER ddb
EXPOSE 8080
# Konfigurasi lewat environment: DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, DATA_KEY, EXPOSE_OTP, SEED_DEMO_DATA
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

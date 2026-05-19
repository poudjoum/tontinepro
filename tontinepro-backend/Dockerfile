# ── Stage 1 : Build (Maven + JDK 21) ────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Résolution des dépendances en cache (layer réutilisé si pom.xml inchangé)
COPY pom.xml ./
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2 : Runtime (JRE 21 minimal) ──────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Utilisateur non-root
RUN addgroup -S tontine && adduser -S tontine -G tontine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown tontine:tontine app.jar

USER tontine

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="shubh"
LABEL description="App Vault — Centralized Credential Vault for Microservices"

WORKDIR /app

COPY vault-server/target/vault-server-1.0.0.jar app.jar

# Default environment variables
ENV VAULT_MASTER_KEY=change-this-to-a-base64-encoded-32-byte-key
ENV VAULT_ADMIN_TOKEN=vault-admin-bootstrap-token-change-me
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vault_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV SERVER_PORT=8200

EXPOSE 8200

ENTRYPOINT ["java", "-jar", "app.jar"]

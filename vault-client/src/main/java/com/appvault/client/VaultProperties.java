package com.appvault.client;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the Vault Client.
 * 
 * In your application.yml, add:
 *   vault:
 *     url: http://localhost:8200
 *     token: your-service-token
 *     environment: dev
 *     secret-paths:
 *       - database/credentials
 *       - api/payment
 */
@Getter
@Setter
public class VaultProperties {

    /**
     * URL of the vault server (e.g., http://localhost:8200)
     */
    private String url = "http://localhost:8200";

    /**
     * Service token for authentication with the vault
     */
    private String token;

    /**
     * Environment this service runs in (dev, staging, prod)
     */
    private String environment = "dev";

    /**
     * Secret paths to fetch at startup and inject as Spring properties.
     * Each path is fetched from the vault, and all key-value pairs under 
     * that path become Spring properties.
     * 
     * Example: path "database/credentials" with keys "username" and "password"
     * becomes Spring properties:
     *   database.credentials.username = ...
     *   database.credentials.password = ...
     */
    private String[] secretPaths = {};

    /**
     * Whether vault integration is enabled (default: true)
     */
    private boolean enabled = true;

    /**
     * Cache TTL in seconds (default: 300 = 5 minutes)
     */
    private int cacheTtlSeconds = 300;

    /**
     * Whether to enable background refresh (default: true)
     */
    private boolean backgroundRefresh = true;

    /**
     * Whether to fail startup if vault is unreachable (default: false)
     * Set to true in production!
     */
    private boolean failOnError = false;

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 10000;
}

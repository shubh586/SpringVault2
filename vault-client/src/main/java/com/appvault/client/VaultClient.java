package com.appvault.client;

import com.appvault.client.cache.CredentialCache;
import com.appvault.client.model.VaultApiResponse;
import com.appvault.client.model.VaultSecretResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Vault Client — the main class for communicating with the vault server.
 * 
 * Usage in any Spring Boot service:
 * 
 * @Autowired
 *            private VaultClient vaultClient;
 * 
 *            String password = vaultClient.getSecret("database/prod",
 *            "password");
 */
public class VaultClient {

    private static final Logger log = LoggerFactory.getLogger(VaultClient.class);

    private final String vaultUrl;
    private final String vaultToken;
    private final String environment;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final CredentialCache cache;

    public VaultClient(VaultProperties properties) {
        this.vaultUrl = properties.getUrl().replaceAll("/$", ""); // strip trailing slash
        this.vaultToken = properties.getToken();
        this.environment = properties.getEnvironment();
        this.objectMapper = new ObjectMapper();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .build();
        this.requestTimeout = Duration.ofMillis(properties.getReadTimeout());

        // Initialize cache
        this.cache = new CredentialCache(
                properties.getCacheTtlSeconds(),
                properties.isBackgroundRefresh() ? this : null);
        log.info("🔐 VaultClient initialized — url={}, env={}", vaultUrl, environment);
    }

    /**
     * Get a single secret value.
     * Checks cache first, then fetches from vault.
     *
     * @param path Secret path (e.g., "database/prod")
     * @param key  Secret key (e.g., "password")
     * @return Decrypted secret value
     */
    public String getSecret(String path, String key) {
        String cacheKey = path + "/" + key;

        // Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Fetch from vault
        try {
            URI uri = buildUri("/api/v1/secrets", Map.of(
                    "path", path,
                    "key", key,
                    "env", environment));

            HttpRequest request = baseRequest(uri)
                    .header("X-Vault-Token", vaultToken)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                VaultApiResponse<VaultSecretResponse> apiResponse = objectMapper.readValue(
                        response.body(),
                        objectMapper.getTypeFactory().constructParametricType(
                                VaultApiResponse.class, VaultSecretResponse.class));

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    String value = apiResponse.getData().getValue();
                    cache.put(cacheKey, value);
                    return value;
                }
            }

            log.error("Failed to fetch secret {}/{}: status={}", path, key, response.statusCode());
            return null;
        } catch (Exception e) {
            log.error("Error fetching secret {}/{}: {}", path, key, e.getMessage());
            return null;
        }
    }

    /**
     * Get all secrets under a path.
     *
     * @param path Secret path (e.g., "database/credentials")
     * @return Map of key → value pairs
     */
    public Map<String, String> getSecretsByPath(String path) {
        try {
            URI uri = buildUri("/api/v1/secrets/path", Map.of(
                    "path", path,
                    "env", environment));

            HttpRequest request = baseRequest(uri)
                    .header("X-Vault-Token", vaultToken)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse as list of secrets
                var typeRef = objectMapper.getTypeFactory().constructParametricType(
                        VaultApiResponse.class,
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, VaultSecretResponse.class));

                VaultApiResponse<List<VaultSecretResponse>> apiResponse = objectMapper.readValue(response.body(),
                        typeRef);

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    Map<String, String> secrets = new LinkedHashMap<>();
                    for (VaultSecretResponse secret : apiResponse.getData()) {
                        secrets.put(secret.getKey(), secret.getValue());
                        cache.put(path + "/" + secret.getKey(), secret.getValue());
                    }
                    return secrets;
                }
            }

            log.error("Failed to fetch secrets for path {}: status={}", path, response.statusCode());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error fetching secrets for path {}: {}", path, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Batch fetch multiple secrets in a single HTTP call.
     * Used during application startup for performance.
     *
     * @param pathKeys List of "path/key" strings
     * @return Map of "path/key" → value
     */
    public Map<String, String> batchGetSecrets(List<String> pathKeys) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paths", pathKeys);
            requestBody.put("environment", environment);

            URI uri = buildUri("/api/v1/secrets/batch", Collections.emptyMap());

            HttpRequest request = baseRequest(uri)
                    .header("X-Vault-Token", vaultToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var typeRef = objectMapper.getTypeFactory().constructParametricType(
                        VaultApiResponse.class,
                        objectMapper.getTypeFactory().constructMapType(
                                LinkedHashMap.class, String.class, String.class));

                VaultApiResponse<Map<String, String>> apiResponse = objectMapper.readValue(response.body(), typeRef);

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    // Cache all results
                    apiResponse.getData().forEach(cache::put);
                    return apiResponse.getData();
                }
            }

            log.error("Batch fetch failed: status={}", response.statusCode());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error in batch fetch: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get the vault URL (used internally for health checks).
     */
    public String getVaultUrl() {
        return vaultUrl;
    }

    /**
     * Get the configured environment.
     */
    public String getEnvironment() {
        return environment;
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout);
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(vaultUrl).append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                first = false;
                url.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(entry.getValue() == null ? "" : entry.getValue()));
            }
        }
        return URI.create(url.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

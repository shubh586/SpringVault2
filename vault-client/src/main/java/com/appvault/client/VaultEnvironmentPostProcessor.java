package com.appvault.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.*;

/**
 * ════════════════════════════════════════════════════════════════════
 *  THIS IS THE KEY INTEGRATION CLASS
 * ════════════════════════════════════════════════════════════════════
 * 
 * EnvironmentPostProcessor runs BEFORE the ApplicationContext is created.
 * This means secrets are injected BEFORE application.yaml is fully processed.
 * 
 * HOW IT WORKS:
 * 1. Spring Boot starts
 * 2. This class runs (before any @Bean, @Configuration, etc.)
 * 3. Reads vault.url, vault.token, vault.environment, vault.secret-paths from config
 * 4. Fetches all specified secrets from the vault
 * 5. Injects them as Spring properties
 * 
 * RESULT:
 * Your application.yml can reference vault secrets just like normal properties:
 *   
 *   spring:
 *     datasource:
 *       password: ${database.credentials.password}
 *       username: ${database.credentials.username}
 *   
 * Where "database/credentials" is a vault path containing keys "password" and "username".
 * 
 * The developer doesn't need to write ANY code — just add the vault-client dependency
 * and configure the vault connection in application.yml.
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(VaultEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "vaultProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Check if vault is enabled
        String enabled = environment.getProperty("vault.enabled", "true");
        if ("false".equalsIgnoreCase(enabled)) {
            log.info("Vault integration disabled");
            return;
        }

        String vaultUrl = environment.getProperty("vault.url");
        String vaultToken = environment.getProperty("vault.token");
        String vaultEnv = environment.getProperty("vault.environment", "dev");
        String secretPathsStr = environment.getProperty("vault.secret-paths");
        String failOnErrorStr = environment.getProperty("vault.fail-on-error", "false");
        boolean failOnError = "true".equalsIgnoreCase(failOnErrorStr);

        // If vault URL or token not configured, skip silently
        if (vaultUrl == null || vaultUrl.isBlank() || vaultToken == null || vaultToken.isBlank()) {
            String message = "Vault URL or token not configured";
            if (failOnError) {
                throw new RuntimeException(message);
            }
            log.debug("{} — skipping vault integration", message);
            return;
        }

        // Parse secret paths
        String[] secretPaths = {};
        if (secretPathsStr != null && !secretPathsStr.isBlank()) {
            secretPaths = secretPathsStr.split(",");
        }

        // Also check array-style properties: vault.secret-paths[0], vault.secret-paths[1], etc.
        if (secretPaths.length == 0) {
            List<String> pathList = new ArrayList<>();
            for (int i = 0; i < 50; i++) { // support up to 50 paths
                String path = environment.getProperty("vault.secret-paths[" + i + "]");
                if (path == null) break;
                pathList.add(path.trim());
            }
            if (!pathList.isEmpty()) {
                secretPaths = pathList.toArray(new String[0]);
            }
        }

        if (secretPaths.length == 0) {
            log.debug("No vault.secret-paths configured — skipping vault secret fetch");
            return;
        }

        log.info("🔐 Vault: Fetching secrets from {} for env={} ({} paths)",
                vaultUrl, vaultEnv, secretPaths.length);

        try {
            // Create a minimal VaultProperties for the client
            VaultProperties props = new VaultProperties();
            props.setUrl(vaultUrl);
            props.setToken(vaultToken);
            props.setEnvironment(vaultEnv);
            props.setBackgroundRefresh(false); // No refresh during bootstrap
            props.setConnectTimeout(
                    Integer.parseInt(environment.getProperty("vault.connect-timeout", "5000")));
            props.setReadTimeout(
                    Integer.parseInt(environment.getProperty("vault.read-timeout", "10000")));

            VaultClient client = new VaultClient(props);

            // Fetch secrets for each path and convert to Spring properties
            Map<String, Object> vaultSecrets = new LinkedHashMap<>();

            for (String path : secretPaths) {
                path = path.trim();
                try {
                    Map<String, String> secrets = client.getSecretsByPath(path);

                    if (secrets.isEmpty()) {
                        log.warn("Vault: No secrets found at path '{}' for env='{}'", path, vaultEnv);
                        continue;
                    }

                    // Convert vault path to Spring property prefix
                    // e.g., "database/credentials" → "database.credentials."
                    String propertyPrefix = path.replace("/", ".") + ".";

                    for (Map.Entry<String, String> entry : secrets.entrySet()) {
                        String propertyName = propertyPrefix + entry.getKey();
                        vaultSecrets.put(propertyName, entry.getValue());
                        log.debug("Vault: Injected property '{}'", propertyName);
                    }

                    log.info("Vault: Loaded {} secrets from path '{}'", secrets.size(), path);
                } catch (Exception e) {
                    log.error("Vault: Failed to fetch path '{}': {}", path, e.getMessage());
                    if (failOnError) {
                        throw new RuntimeException("Failed to fetch vault secrets from path: " + path, e);
                    }
                }
            }

            if (!vaultSecrets.isEmpty()) {
                // Add vault properties with HIGHEST priority (overrides application.yml)
                MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, vaultSecrets);
                environment.getPropertySources().addFirst(propertySource);
                log.info("🔐 Vault: Successfully injected {} properties into Spring Environment",
                        vaultSecrets.size());
            }

        } catch (Exception e) {
            log.error("🔐 Vault: Failed to initialize — {}", e.getMessage());
            if (failOnError) {
                throw new RuntimeException("Vault initialization failed", e);
            }
        }
    }

    @Override
    public int getOrder() {
        // Run with high priority — before other post processors
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

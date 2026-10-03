package com.appvault.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for the Vault Client.
 * 
 * When vault-client jar is on the classpath and vault.enabled=true,
 * a VaultClient bean is automatically created and available for injection.
 * 
 * Developers can just do:
 *   @Autowired
 *   private VaultClient vaultClient;
 */
@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = true)
public class VaultAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VaultProperties vaultProperties(Environment environment) {
        // Bind properties from the environment
        VaultProperties props = new VaultProperties();

        String url = environment.getProperty("vault.url");
        if (url != null) props.setUrl(url);

        String token = environment.getProperty("vault.token");
        if (token != null) props.setToken(token);

        String env = environment.getProperty("vault.environment", "dev");
        props.setEnvironment(env);

        String cacheTtl = environment.getProperty("vault.cache-ttl-seconds", "300");
        props.setCacheTtlSeconds(Integer.parseInt(cacheTtl));

        String bgRefresh = environment.getProperty("vault.background-refresh", "true");
        props.setBackgroundRefresh("true".equalsIgnoreCase(bgRefresh));

        String failOnError = environment.getProperty("vault.fail-on-error", "false");
        props.setFailOnError("true".equalsIgnoreCase(failOnError));

        return props;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "vault.url")
    public VaultClient vaultClient(VaultProperties vaultProperties) {
        return new VaultClient(vaultProperties);
    }
}

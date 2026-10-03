package com.appvault.server.config;

import com.appvault.server.model.AuditLog;
import com.appvault.server.model.Policy;
import com.appvault.server.model.ServiceToken;
import com.appvault.server.repository.PolicyRepository;
import com.appvault.server.repository.ServiceTokenRepository;
import com.appvault.server.service.AuditService;
import com.appvault.server.service.ServiceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the vault with bootstrap data on first run:
 *  - Admin token hash
 *  - Default policies
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ServiceTokenRepository serviceTokenRepository;
    private final PolicyRepository policyRepository;
    private final ServiceTokenService serviceTokenService;
    private final AuditService auditService;

    @Value("${vault.admin.token}")
    private String adminToken;
    @Value("${vault.bootstrap.create-dev-policy:false}")
    private boolean createDevPolicy;

    @Override
    public void run(String... args) {
        log.info("═══════════════════════════════════════════");
        log.info("   🔐 App Vault - Credential Vault v1.0   ");
        log.info("═══════════════════════════════════════════");

        // Create an optional default policy only when explicitly enabled.
        if (createDevPolicy && policyRepository.count() == 0) {
            // Default wide-open policy for dev
            Policy devPolicy = Policy.builder()
                    .name("dev-allow-all")
                    .serviceName("*")
                    .environment("dev")
                    .region("*")
                    .allowedPaths("*")
                    .effect(Policy.PolicyEffect.ALLOW)
                    .active(true)
                    .build();
            policyRepository.save(devPolicy);

            log.info("✅ Created default dev policy: dev-allow-all (allows all services in dev)");
        } else if (!createDevPolicy) {
            log.info("ℹ️ Skipping default dev policy bootstrap (vault.bootstrap.create-dev-policy=false)");
        }

        log.info("═══════════════════════════════════════════");
        log.info("  Admin Token: Use X-Vault-Token header    ");
        log.info("  with the configured vault.admin.token    ");
        log.info("  to manage policies and generate service  ");
        log.info("  tokens.                                  ");
        log.info("═══════════════════════════════════════════");
        log.info("  Server running on port 8200              ");
        log.info("  Health: GET /api/v1/health               ");
        log.info("═══════════════════════════════════════════");
    }
}

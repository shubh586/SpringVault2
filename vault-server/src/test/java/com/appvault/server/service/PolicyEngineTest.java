package com.appvault.server.service;

import com.appvault.server.model.Policy;
import com.appvault.server.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyEngineTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyEngine policyEngine;

    private Policy allowDatabasePolicy;
    private Policy denyProdSecretsPolicy;

    @BeforeEach
    void setUp() {
        allowDatabasePolicy = Policy.builder()
                .id(1L)
                .name("order-service-db-access")
                .serviceName("order-service")
                .environment("prod")
                .region("us-east")
                .allowedPaths("database/*")
                .effect(Policy.PolicyEffect.ALLOW)
                .active(true)
                .build();

        denyProdSecretsPolicy = Policy.builder()
                .id(2L)
                .name("deny-financial-keys")
                .serviceName("order-service")
                .environment("prod")
                .region("us-east")
                .allowedPaths("database/finance/*")
                .effect(Policy.PolicyEffect.DENY)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should ALLOW access when policy matches service, env, region, and wildcard path")
    void testAllowAccessWithWildcardPath() {
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(allowDatabasePolicy));

        boolean hasAccess = policyEngine.evaluateAccess(
                "order-service", "prod", "us-east", "database/postgres/password");

        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("Should DENY access when explicit DENY policy overrides ALLOW policy")
    void testDenyOverridesAllow() {
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(allowDatabasePolicy, denyProdSecretsPolicy));

        boolean hasAccess = policyEngine.evaluateAccess(
                "order-service", "prod", "us-east", "database/finance/stripe-key");

        assertThat(hasAccess).isFalse();
    }

    @Test
    @DisplayName("Should DENY access when environment does not match (Environment Isolation)")
    void testEnvironmentIsolation() {
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(allowDatabasePolicy));

        // Same service and path, but staging environment instead of prod
        boolean hasAccess = policyEngine.evaluateAccess(
                "order-service", "staging", "us-east", "database/postgres/password");

        assertThat(hasAccess).isFalse();
    }

    @Test
    @DisplayName("Should DENY access by default when no matching policies exist")
    void testDefaultDeny() {
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(allowDatabasePolicy));

        boolean hasAccess = policyEngine.evaluateAccess(
                "unknown-service", "prod", "us-east", "database/postgres/password");

        assertThat(hasAccess).isFalse();
    }

    @Test
    @DisplayName("Should match wildcard attribute values (*) in policies")
    void testWildcardAttributes() {
        Policy wildcardPolicy = Policy.builder()
                .id(3L)
                .name("global-read-app-info")
                .serviceName("*")
                .environment("*")
                .region("*")
                .allowedPaths("common/app/*")
                .effect(Policy.PolicyEffect.ALLOW)
                .active(true)
                .build();

        when(policyRepository.findByActiveTrue()).thenReturn(List.of(wildcardPolicy));

        boolean hasAccess = policyEngine.evaluateAccess(
                "any-service", "dev", "eu-west", "common/app/version");

        assertThat(hasAccess).isTrue();
    }
}

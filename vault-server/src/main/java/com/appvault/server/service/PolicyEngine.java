package com.appvault.server.service;

import com.appvault.server.model.Policy;
import com.appvault.server.model.dto.PolicyRequest;
import com.appvault.server.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * ABAC (Attribute-Based Access Control) Policy Engine.
 *
 * Evaluates whether a service can access a specific secret based on:
 *   - Service name
 *   - Environment (dev, staging, prod)
 *   - Region (us-east, eu-west, etc.)
 *   - Secret path (supports wildcard matching)
 *
 * DENY policies always take precedence over ALLOW policies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyRepository policyRepository;

    /**
     * Evaluate whether a service has access to a specific secret path.
     *
     * @param serviceName The requesting service
     * @param environment The service's environment
     * @param region      The service's region
     * @param secretPath  The full path of the secret (e.g., "database/prod/password")
     * @return true if access is allowed, false otherwise
     */
    public boolean evaluateAccess(String serviceName, String environment,
                                   String region, String secretPath) {
        // Fetch all active policies for this specific service + wildcard policies
        List<Policy> policies = policyRepository.findByActiveTrue();

        // Filter policies that apply to this service
        List<Policy> applicablePolicies = policies.stream()
                .filter(p -> matchesAttribute(p.getServiceName(), serviceName))
                .filter(p -> matchesAttribute(p.getEnvironment(), environment))
                .filter(p -> matchesAttribute(p.getRegion(), region))
                .toList();

        if (applicablePolicies.isEmpty()) {
            log.warn("No policies found for service={}, env={}, region={}, path={}",
                    serviceName, environment, region, secretPath);
            return false; // Default deny — no policy means no access
        }

        // Check for explicit DENY first (DENY always wins)
        boolean hasDeny = applicablePolicies.stream()
                .filter(p -> p.getEffect() == Policy.PolicyEffect.DENY)
                .anyMatch(p -> matchesPath(p.getAllowedPaths(), secretPath));

        if (hasDeny) {
            log.info("DENY policy matched for service={}, path={}", serviceName, secretPath);
            return false;
        }

        // Check for ALLOW
        boolean hasAllow = applicablePolicies.stream()
                .filter(p -> p.getEffect() == Policy.PolicyEffect.ALLOW)
                .anyMatch(p -> matchesPath(p.getAllowedPaths(), secretPath));

        if (hasAllow) {
            log.debug("ALLOW policy matched for service={}, path={}", serviceName, secretPath);
            return true;
        }

        log.warn("No matching ALLOW policy for service={}, path={}", serviceName, secretPath);
        return false; // Default deny
    }

    /**
     * Match an attribute value against a policy attribute.
     * "*" in the policy matches everything.
     */
    private boolean matchesAttribute(String policyValue, String requestValue) {
        if (policyValue == null || policyValue.equals("*")) {
            return true;
        }
        // Also match if the REQUEST side is wildcard (e.g., admin with env="*")
        if (requestValue != null && requestValue.equals("*")) {
            return true;
        }
        return policyValue.equalsIgnoreCase(requestValue);
    }

    /**
     * Check if the requested path matches any of the allowed paths in the policy.
     * Supports wildcard matching:
     *   "database/*" matches "database/password", "database/username", etc.
     *   "*" matches everything
     *   "database/prod/*" matches "database/prod/password" but not "database/dev/password"
     */
    private boolean matchesPath(String allowedPathsCsv, String requestedPath) {
        if (allowedPathsCsv == null) return false;

        String[] allowedPaths = allowedPathsCsv.split(",");
        return Arrays.stream(allowedPaths)
                .map(String::trim)
                .anyMatch(pattern -> pathMatchesPattern(pattern, requestedPath));
    }

    /**
     * Wildcard path matching.
     */
    private boolean pathMatchesPattern(String pattern, String path) {
        if (pattern.equals("*")) return true;
        if (pattern.equals(path)) return true;

        // Handle trailing wildcard: "database/*" matches "database/anything"
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix + "/") || path.equals(prefix);
        }

        // Handle wildcard in the middle: "*/prod/*"
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }

        return false;
    }

    /**
     * Create a new policy.
     */
    public Policy createPolicy(PolicyRequest request) {
        if (policyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Policy with name '" + request.getName() + "' already exists");
        }

        Policy policy = Policy.builder()
                .name(request.getName())
                .serviceName(request.getServiceName())
                .environment(request.getEnvironment() != null ? request.getEnvironment() : "*")
                .region(request.getRegion() != null ? request.getRegion() : "*")
                .allowedPaths(request.getAllowedPaths())
                .effect(request.getEffect() != null ?
                        Policy.PolicyEffect.valueOf(request.getEffect().toUpperCase()) :
                        Policy.PolicyEffect.ALLOW)
                .active(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * Get all active policies.
     */
    public List<Policy> getAllPolicies() {
        return policyRepository.findByActiveTrue();
    }

    /**
     * Delete (deactivate) a policy.
     */
    public void deletePolicy(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + id));
        policy.setActive(false);
        policyRepository.save(policy);
    }
}

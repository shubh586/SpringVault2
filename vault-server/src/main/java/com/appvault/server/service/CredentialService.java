package com.appvault.server.service;

import com.appvault.server.model.AuditLog;
import com.appvault.server.model.Credential;
import com.appvault.server.model.dto.SecretRequest;
import com.appvault.server.model.dto.SecretResponse;
import com.appvault.server.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core credential management service.
 * Handles storing, retrieving, rotating, and deleting secrets.
 * All operations are audited and policy-checked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final EncryptionService encryptionService;
    private final PolicyEngine policyEngine;
    private final AuditService auditService;

    /**
     * Store a new secret (encrypts before storing).
     */
    @Transactional
    public SecretResponse storeSecret(SecretRequest request, String callerService,
                                       String callerEnv, String callerRegion, String ipAddress) {
        String fullPath = request.getPath();
        String targetEnvironment = resolveTargetEnvironment(request.getEnvironment(), callerService, callerEnv);

        // Policy check
        if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, fullPath)) {
            auditService.logAccess(callerService, AuditLog.AuditAction.WRITE,
                    fullPath, callerEnv, callerRegion, ipAddress, false, "Policy denied");
            throw new SecurityException("Access denied by policy");
        }

        // Check if credential already exists
        if (credentialRepository.existsByPathAndSecretKeyAndEnvironment(
                request.getPath(), request.getKey(), targetEnvironment)) {
            throw new IllegalArgumentException(
                    "Credential already exists for the requested location. Use PUT to rotate.");
        }

        // Encrypt the value
        EncryptionService.EncryptionResult encrypted = encryptionService.encrypt(request.getValue());

        Credential credential = Credential.builder()
                .path(request.getPath())
                .secretKey(request.getKey())
                .encryptedValue(encrypted.encryptedValue())
                .iv(encrypted.iv())
                .environment(targetEnvironment)
                .region(request.getRegion())
                .description(request.getDescription())
                .version(1)
                .build();

        credentialRepository.save(credential);

        // Audit
        auditService.logAccess(callerService, AuditLog.AuditAction.WRITE,
                fullPath + "/" + request.getKey(), callerEnv, callerRegion, ipAddress, true, null);

        log.info("Secret stored: {}/{} [{}]", request.getPath(), request.getKey(), targetEnvironment);

        return toResponse(credential, request.getValue());
    }

    /**
     * Retrieve a secret by path and key (decrypts before returning).
     */
    public SecretResponse getSecret(String path, String key, String environment,
                                     String callerService, String callerEnv,
                                     String callerRegion, String ipAddress) {
        String targetEnvironment = resolveTargetEnvironment(environment, callerService, callerEnv);
        // Policy check
        if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, path)) {
            auditService.logAccess(callerService, AuditLog.AuditAction.READ,
                    path, callerEnv, callerRegion, ipAddress, false, "Policy denied");
            throw new SecurityException("Access denied by policy");
        }

        Credential credential = credentialRepository
                .findByPathAndSecretKeyAndEnvironment(path, key, targetEnvironment)
                .orElseThrow(() -> {
                    auditService.logAccess(callerService, AuditLog.AuditAction.READ,
                            path + "/" + key, callerEnv, callerRegion, ipAddress, false, "Not found");
                    return new NoSuchElementException("Secret not found");
                });

        String decryptedValue = encryptionService.decrypt(credential.getEncryptedValue(), credential.getIv());

        // Audit
        auditService.logAccess(callerService, AuditLog.AuditAction.READ,
                path + "/" + key, callerEnv, callerRegion, ipAddress, true, null);

        return toResponse(credential, decryptedValue);
    }

    /**
     * Retrieve all secrets under a path for a given environment.
     */
    public List<SecretResponse> getSecretsByPath(String path, String environment,
                                                  String callerService, String callerEnv,
                                                  String callerRegion, String ipAddress) {
        String targetEnvironment = resolveTargetEnvironment(environment, callerService, callerEnv);
        // Policy check
        if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, path)) {
            auditService.logAccess(callerService, AuditLog.AuditAction.READ,
                    path, callerEnv, callerRegion, ipAddress, false, "Policy denied");
            throw new SecurityException("Access denied by policy");
        }

        List<Credential> credentials = credentialRepository.findByPathAndEnvironment(path, targetEnvironment);

        // Audit
        auditService.logAccess(callerService, AuditLog.AuditAction.BATCH_READ,
                path, callerEnv, callerRegion, ipAddress, true, null);

        return credentials.stream()
                .map(c -> {
                    String decrypted = encryptionService.decrypt(c.getEncryptedValue(), c.getIv());
                    return toResponse(c, decrypted);
                })
                .collect(Collectors.toList());
    }

    /**
     * Batch fetch secrets — critical for startup performance.
     * Fetches multiple secrets in a single request.
     */
    public Map<String, String> batchGetSecrets(List<String> pathKeys, String environment,
                                                String callerService, String callerEnv,
                                                String callerRegion, String ipAddress) {
        String targetEnvironment = resolveTargetEnvironment(environment, callerService, callerEnv);
        Map<String, String> results = new LinkedHashMap<>();
        int failureCount = 0;

        for (String pathKey : pathKeys) {
            String[] parts = pathKey.split("/");
            if (parts.length < 2) {
                failureCount++;
                continue;
            }

            String key = parts[parts.length - 1];
            String path = pathKey.substring(0, pathKey.lastIndexOf("/"));

            try {
                // Policy check per secret
                if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, path)) {
                    log.warn("Batch: policy denied for {}", pathKey);
                    failureCount++;
                    continue;
                }

                credentialRepository.findByPathAndSecretKeyAndEnvironment(path, key, targetEnvironment)
                        .ifPresent(credential -> {
                            String decrypted = encryptionService.decrypt(
                                    credential.getEncryptedValue(), credential.getIv());
                            results.put(pathKey, decrypted);
                        });
                if (!results.containsKey(pathKey)) {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Batch: error fetching {}: {}", pathKey, e.getMessage());
                failureCount++;
            }
        }

        // Audit the batch operation
        auditService.logAccess(callerService, AuditLog.AuditAction.BATCH_READ,
                "batch[" + pathKeys.size() + " keys]", callerEnv, callerRegion, ipAddress,
                failureCount == 0, failureCount == 0 ? null : "Partial batch failures");

        return results;
    }

    /**
     * Rotate (update) a secret — creates a new encrypted version.
     */
    @Transactional
    public SecretResponse rotateSecret(String path, String key, String newValue,
                                        String environment, String callerService,
                                        String callerEnv, String callerRegion, String ipAddress) {
        String targetEnvironment = resolveTargetEnvironment(environment, callerService, callerEnv);
        // Policy check
        if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, path)) {
            auditService.logAccess(callerService, AuditLog.AuditAction.ROTATE,
                    path + "/" + key, callerEnv, callerRegion, ipAddress, false, "Policy denied");
            throw new SecurityException("Access denied by policy");
        }

        Credential credential = credentialRepository
                .findByPathAndSecretKeyAndEnvironment(path, key, targetEnvironment)
                .orElseThrow(() -> new NoSuchElementException("Secret not found"));

        // Re-encrypt with new value (gets a new IV)
        EncryptionService.EncryptionResult encrypted = encryptionService.encrypt(newValue);
        credential.setEncryptedValue(encrypted.encryptedValue());
        credential.setIv(encrypted.iv());
        credential.setVersion(credential.getVersion() + 1);

        credentialRepository.save(credential);

        // Audit
        auditService.logAccess(callerService, AuditLog.AuditAction.ROTATE,
                path + "/" + key, callerEnv, callerRegion, ipAddress, true, null);

        log.info("Secret rotated: {}/{} to version {} [{}]",
                path, key, credential.getVersion(), targetEnvironment);

        return toResponse(credential, newValue);
    }

    /**
     * Delete a secret.
     */
    @Transactional
    public void deleteSecret(String path, String key, String environment,
                              String callerService, String callerEnv,
                              String callerRegion, String ipAddress) {
        String targetEnvironment = resolveTargetEnvironment(environment, callerService, callerEnv);
        // Policy check
        if (!policyEngine.evaluateAccess(callerService, callerEnv, callerRegion, path)) {
            auditService.logAccess(callerService, AuditLog.AuditAction.DELETE,
                    path + "/" + key, callerEnv, callerRegion, ipAddress, false, "Policy denied");
            throw new SecurityException("Access denied by policy");
        }

        Credential credential = credentialRepository
                .findByPathAndSecretKeyAndEnvironment(path, key, targetEnvironment)
                .orElseThrow(() -> new NoSuchElementException("Secret not found"));

        credentialRepository.delete(credential);

        // Audit
        auditService.logAccess(callerService, AuditLog.AuditAction.DELETE,
                path + "/" + key, callerEnv, callerRegion, ipAddress, true, null);

        log.info("Secret deleted: {}/{} [{}]", path, key, targetEnvironment);
    }

    /**
     * Get total count of stored secrets.
     */
    public long getTotalSecrets() {
        return credentialRepository.count();
    }

    /**
     * Map entity to response DTO.
     */
    private SecretResponse toResponse(Credential credential, String decryptedValue) {
        return SecretResponse.builder()
                .path(credential.getPath())
                .key(credential.getSecretKey())
                .value(decryptedValue)
                .environment(credential.getEnvironment())
                .region(credential.getRegion())
                .description(credential.getDescription())
                .version(credential.getVersion())
                .createdAt(credential.getCreatedAt())
                .updatedAt(credential.getUpdatedAt())
                .build();
    }

    private String resolveTargetEnvironment(String requestedEnvironment, String callerService, String callerEnvironment) {
        if ("admin".equalsIgnoreCase(callerService)) {
            return normalizeEnvironment(requestedEnvironment, callerEnvironment);
        }
        String requested = normalizeEnvironment(requestedEnvironment, callerEnvironment);
        if (!requested.equals(callerEnvironment)) {
            throw new SecurityException("Cross-environment access is not allowed");
        }
        return requested;
    }

    private String normalizeEnvironment(String requestedEnvironment, String fallbackEnvironment) {
        if (requestedEnvironment == null || requestedEnvironment.isBlank()) {
            return fallbackEnvironment;
        }
        return requestedEnvironment.trim();
    }
}

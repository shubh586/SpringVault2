package com.appvault.server.controller;

import com.appvault.server.model.dto.*;
import com.appvault.server.security.TokenAuthenticationFilter.VaultPrincipal;
import com.appvault.server.service.CredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/secrets")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * Store a new secret.
     * POST /api/v1/secrets
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SecretResponse>> storeSecret(
            @Valid @RequestBody SecretRequest request,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        SecretResponse response = credentialService.storeSecret(
                request,
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Secret stored successfully", response));
    }

    /**
     * Get a specific secret by path and key.
     * GET /api/v1/secrets?path=database/prod&key=password&env=prod
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SecretResponse>> getSecret(
            @RequestParam String path,
            @RequestParam String key,
            @RequestParam(required = false) String env,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        SecretResponse response = credentialService.getSecret(
                path, key, env,
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all secrets under a path.
     * GET /api/v1/secrets/path?path=database/prod&env=prod
     */
    @GetMapping("/path")
    public ResponseEntity<ApiResponse<List<SecretResponse>>> getSecretsByPath(
            @RequestParam String path,
            @RequestParam(required = false) String env,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        List<SecretResponse> responses = credentialService.getSecretsByPath(
                path, env,
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Batch fetch multiple secrets.
     * POST /api/v1/secrets/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, String>>> batchGetSecrets(
            @RequestBody BatchSecretRequest request,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        Map<String, String> results = credentialService.batchGetSecrets(
                request.getPaths(),
                request.getEnvironment() != null ? request.getEnvironment() : principal.environment(),
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Rotate (update) an existing secret.
     * PUT /api/v1/secrets
     */
    @PutMapping
    public ResponseEntity<ApiResponse<SecretResponse>> rotateSecret(
            @Valid @RequestBody SecretRequest request,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        SecretResponse response = credentialService.rotateSecret(
                request.getPath(), request.getKey(), request.getValue(),
                request.getEnvironment(),
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success("Secret rotated successfully", response));
    }

    /**
     * Delete a secret.
     * DELETE /api/v1/secrets?path=database/prod&key=password&env=prod
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteSecret(
            @RequestParam String path,
            @RequestParam String key,
            @RequestParam(required = false) String env,
            @AuthenticationPrincipal VaultPrincipal principal,
            HttpServletRequest httpRequest) {

        credentialService.deleteSecret(path, key, env,
                principal.serviceName(),
                principal.environment(),
                principal.region(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success("Secret deleted successfully", null));
    }
}

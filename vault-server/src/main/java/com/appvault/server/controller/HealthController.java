package com.appvault.server.controller;

import com.appvault.server.model.dto.ApiResponse;
import com.appvault.server.service.AuditService;
import com.appvault.server.service.CredentialService;
import com.appvault.server.service.PolicyEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check and dashboard data endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final CredentialService credentialService;
    private final AuditService auditService;
    private final PolicyEngine policyEngine;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "vault-server");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "1.0.0");
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Dashboard summary endpoint (admin-auth protected by SecurityConfig).
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSecrets", credentialService.getTotalSecrets());
        summary.put("totalPolicies", policyEngine.getAllPolicies().size());
        summary.put("auditStats", auditService.getStatistics());
        summary.put("serverTime", LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}

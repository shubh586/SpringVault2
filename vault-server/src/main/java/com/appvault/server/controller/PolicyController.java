package com.appvault.server.controller;

import com.appvault.server.model.Policy;
import com.appvault.server.model.dto.ApiResponse;
import com.appvault.server.model.dto.PolicyRequest;
import com.appvault.server.service.PolicyEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyEngine policyEngine;

    /**
     * Create a new access policy.
     * POST /api/v1/policies
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Policy>> createPolicy(
            @Valid @RequestBody PolicyRequest request) {

        Policy policy = policyEngine.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created successfully", policy));
    }

    /**
     * Get all active policies.
     * GET /api/v1/policies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Policy>>> getAllPolicies() {
        List<Policy> policies = policyEngine.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    /**
     * Delete (deactivate) a policy.
     * DELETE /api/v1/policies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        policyEngine.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.success("Policy deactivated successfully", null));
    }
}

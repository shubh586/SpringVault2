package com.appvault.server.controller;

import com.appvault.server.model.AuditLog;
import com.appvault.server.model.dto.ApiResponse;
import com.appvault.server.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Get paginated audit logs.
     * GET /api/v1/audit/logs?page=0&size=20
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLog> logs = auditService.getLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get audit logs filtered by service.
     * GET /api/v1/audit/logs/service/{serviceName}
     */
    @GetMapping("/logs/service/{serviceName}")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogsByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLog> logs = auditService.getLogsByService(serviceName, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get audit logs filtered by action type.
     * GET /api/v1/audit/logs/action/{action}
     */
    @GetMapping("/logs/action/{action}")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogsByAction(
            @PathVariable AuditLog.AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLog> logs = auditService.getLogsByAction(action, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get dashboard statistics.
     * GET /api/v1/audit/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = auditService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

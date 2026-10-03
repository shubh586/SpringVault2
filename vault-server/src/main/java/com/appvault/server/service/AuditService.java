package com.appvault.server.service;

import com.appvault.server.model.AuditLog;
import com.appvault.server.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit Service — records every credential access for traceability and compliance.
 * Uses @Async to avoid blocking the main request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an audit event asynchronously.
     * This method returns immediately; logging happens on a separate thread.
     */
    @Async
    public void logAccess(String serviceName, AuditLog.AuditAction action,
                          String resourcePath, String environment, String region,
                          String ipAddress, boolean success, String failureReason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceName(serviceName)
                    .action(action)
                    .resourcePath(resourcePath)
                    .environment(environment)
                    .region(region)
                    .ipAddress(ipAddress)
                    .success(success)
                    .failureReason(failureReason)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit: {} {} {} by {} [{}]",
                    action, resourcePath, success ? "SUCCESS" : "FAILED", serviceName, environment);
        } catch (Exception e) {
            // Audit logging should never crash the main flow
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Get paginated audit logs, most recent first.
     */
    public Page<AuditLog> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs filtered by service name.
     */
    public Page<AuditLog> getLogsByService(String serviceName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByServiceName(serviceName, pageable);
    }

    /**
     * Get audit logs filtered by action type.
     */
    public Page<AuditLog> getLogsByAction(AuditLog.AuditAction action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * Get audit logs within a time range.
     */
    public Page<AuditLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByTimestampBetween(start, end, pageable);
    }

    /**
     * Get dashboard statistics.
     */
    public Map<String, Object> getStatistics() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", auditLogRepository.count());
        stats.put("accessLast24h", auditLogRepository.countAccessSince(last24Hours));
        stats.put("activeServicesLast24h", auditLogRepository.countActiveServicesSince(last24Hours));
        stats.put("failedAccessLast24h", auditLogRepository.countFailedAccessSince(last24Hours));
        stats.put("accessLast7Days", auditLogRepository.countAccessSince(last7Days));
        stats.put("failedAccessLast7Days", auditLogRepository.countFailedAccessSince(last7Days));

        return stats;
    }
}

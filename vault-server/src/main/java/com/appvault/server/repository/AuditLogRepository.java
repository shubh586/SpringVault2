package com.appvault.server.repository;

import com.appvault.server.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByServiceNameAndTimestampBetween(
            String serviceName, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since")
    long countAccessSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.serviceName) FROM AuditLog a WHERE a.timestamp >= :since")
    long countActiveServicesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.success = false AND a.timestamp >= :since")
    long countFailedAccessSince(@Param("since") LocalDateTime since);
}

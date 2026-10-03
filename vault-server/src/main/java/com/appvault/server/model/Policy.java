package com.appvault.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the policy for identification
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Service name this policy applies to (e.g., "order-service")
     * Use "*" for all services
     */
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * Environment constraint (dev, staging, prod, or "*" for all)
     */
    @Column(nullable = false)
    @Builder.Default
    private String environment = "*";

    /**
     * Region constraint (us-east, eu-west, or "*" for all)
     */
    @Column
    @Builder.Default
    private String region = "*";

    /**
     * Comma-separated list of allowed secret paths
     * Supports wildcards: "database/*", "api/payment/*"
     */
    @Column(name = "allowed_paths", nullable = false, columnDefinition = "TEXT")
    private String allowedPaths;

    /**
     * Policy effect: ALLOW or DENY
     * DENY always takes precedence over ALLOW
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PolicyEffect effect = PolicyEffect.ALLOW;

    /**
     * Whether this policy is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PolicyEffect {
        ALLOW, DENY
    }
}

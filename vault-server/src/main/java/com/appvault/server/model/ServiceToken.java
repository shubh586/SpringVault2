package com.appvault.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_tokens", indexes = {
    @Index(name = "idx_token_hash", columnList = "token_hash", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServiceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the service this token belongs to
     */
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * SHA-256 hash of the actual token (we never store raw tokens)
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /**
     * Environment this token is valid for (dev, staging, prod)
     */
    @Column(nullable = false)
    private String environment;

    /**
     * Region this token is valid for
     */
    @Column
    @Builder.Default
    private String region = "*";

    /**
     * Whether this token is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if this token has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this token is valid (active and not expired)
     */
    public boolean isValid() {
        return active && !isExpired();
    }
}

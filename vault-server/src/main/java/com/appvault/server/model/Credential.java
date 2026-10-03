package com.appvault.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "credentials", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"path", "secret_key"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Logical path for the secret, e.g., "database/prod/password"
     * Used for organizing and accessing secrets hierarchically
     */
    @Column(nullable = false)
    private String path;

    /**
     * Key name within the path, e.g., "password", "username", "api-key"
     */
    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    /**
     * AES-256-GCM encrypted value (Base64 encoded)
     */
    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    /**
     * Initialization Vector used for this encryption (Base64 encoded)
     * Each secret gets a unique IV for security
     */
    @Column(nullable = false)
    private String iv;

    /**
     * Environment this secret belongs to (dev, staging, prod)
     */
    @Column(nullable = false)
    private String environment;

    /**
     * Region/datacenter (us-east, eu-west, ap-south, etc.)
     */
    @Column
    private String region;

    /**
     * Optional description for this credential
     */
    @Column
    private String description;

    /**
     * Version number for tracking rotations
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

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
}

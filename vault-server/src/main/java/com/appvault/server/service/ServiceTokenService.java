package com.appvault.server.service;

import com.appvault.server.model.ServiceToken;
import com.appvault.server.model.dto.TokenRequest;
import com.appvault.server.model.dto.TokenResponse;
import com.appvault.server.repository.ServiceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Manages service authentication tokens.
 * Tokens are generated securely and only stored as SHA-256 hashes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenService {

    private final ServiceTokenRepository serviceTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new service token.
     * Returns the raw token ONCE — it's only stored as a hash.
     */
    public TokenResponse generateToken(TokenRequest request) {
        // Generate a cryptographically secure random token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = "vlt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        String tokenHash = hashToken(rawToken);

        // Deactivate any existing token for this service
        serviceTokenRepository.findByServiceNameAndActiveTrue(request.getServiceName())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    serviceTokenRepository.save(existing);
                });

        ServiceToken serviceToken = ServiceToken.builder()
                .serviceName(request.getServiceName())
                .tokenHash(tokenHash)
                .environment(request.getEnvironment())
                .region(request.getRegion() != null ? request.getRegion() : "*")
                .active(true)
                .expiresAt(request.getExpiryHours() != null ?
                        LocalDateTime.now().plusHours(request.getExpiryHours()) : null)
                .build();

        serviceTokenRepository.save(serviceToken);

        log.info("Generated new token for service: {} [{}]",
                request.getServiceName(), request.getEnvironment());

        return TokenResponse.builder()
                .token(rawToken)
                .serviceName(request.getServiceName())
                .environment(request.getEnvironment())
                .region(serviceToken.getRegion())
                .expiresAt(serviceToken.getExpiresAt())
                .build();
    }

    /**
     * Validate a token and return the associated service identity.
     */
    public Optional<ServiceToken> validateToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        return serviceTokenRepository.findByTokenHashAndActiveTrue(tokenHash)
                .filter(ServiceToken::isValid);
    }

    /**
     * Hash a token using SHA-256.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}

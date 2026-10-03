package com.appvault.server.service;

import com.appvault.server.model.ServiceToken;
import com.appvault.server.model.dto.TokenRequest;
import com.appvault.server.model.dto.TokenResponse;
import com.appvault.server.repository.ServiceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceTokenServiceTest {

    @Mock
    private ServiceTokenRepository serviceTokenRepository;

    @InjectMocks
    private ServiceTokenService serviceTokenService;

    @Test
    @DisplayName("Should generate secure token and store only SHA-256 hash")
    void testGenerateTokenStoresOnlyHash() {
        TokenRequest request = new TokenRequest();
        request.setServiceName("order-service");
        request.setEnvironment("prod");
        request.setRegion("us-east");
        request.setExpiryHours(24);

        when(serviceTokenRepository.findByServiceNameAndActiveTrue("order-service"))
                .thenReturn(Optional.empty());

        TokenResponse response = serviceTokenService.generateToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).startsWith("vlt_");
        assertThat(response.getServiceName()).isEqualTo("order-service");

        // Verify the entity saved to the database does not contain the raw token
        ArgumentCaptor<ServiceToken> captor = ArgumentCaptor.forClass(ServiceToken.class);
        verify(serviceTokenRepository).save(captor.capture());

        ServiceToken savedToken = captor.getValue();
        assertThat(savedToken.getTokenHash()).isNotEqualTo(response.getToken());
        assertThat(savedToken.getTokenHash()).hasSize(64); // SHA-256 hex string is 64 characters
        assertThat(savedToken.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should validate valid active token")
    void testValidateTokenSuccess() {
        String rawToken = "vlt_test_token_1234567890";
        String expectedHash = serviceTokenService.hashToken(rawToken);

        ServiceToken validToken = ServiceToken.builder()
                .serviceName("payment-service")
                .tokenHash(expectedHash)
                .environment("prod")
                .active(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(serviceTokenRepository.findByTokenHashAndActiveTrue(expectedHash))
                .thenReturn(Optional.of(validToken));

        Optional<ServiceToken> result = serviceTokenService.validateToken(rawToken);

        assertThat(result).isPresent();
        assertThat(result.get().getServiceName()).isEqualTo("payment-service");
    }

    @Test
    @DisplayName("Should reject expired token during validation")
    void testValidateExpiredToken() {
        String rawToken = "vlt_expired_token_1234567890";
        String expectedHash = serviceTokenService.hashToken(rawToken);

        ServiceToken expiredToken = ServiceToken.builder()
                .serviceName("payment-service")
                .tokenHash(expectedHash)
                .environment("prod")
                .active(true)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired in the past
                .build();

        when(serviceTokenRepository.findByTokenHashAndActiveTrue(expectedHash))
                .thenReturn(Optional.of(expiredToken));

        Optional<ServiceToken> result = serviceTokenService.validateToken(rawToken);

        assertThat(result).isEmpty();
    }
}

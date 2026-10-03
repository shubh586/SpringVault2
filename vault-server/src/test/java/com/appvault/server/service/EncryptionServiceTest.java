package com.appvault.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    // 32-byte master key encoded in Base64
    private static final String VALID_32_BYTE_KEY = Base64.getEncoder().encodeToString(
            "12345678901234567890123456789012".getBytes());

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterKeyBase64", VALID_32_BYTE_KEY);
        encryptionService.init();
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt plaintext")
    void testEncryptAndDecrypt() {
        String plaintext = "my-super-secret-password-123!";

        EncryptionService.EncryptionResult result = encryptionService.encrypt(plaintext);

        assertThat(result).isNotNull();
        assertThat(result.encryptedValue()).isNotBlank();
        assertThat(result.iv()).isNotBlank();

        String decrypted = encryptionService.decrypt(result.encryptedValue(), result.iv());
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Should produce different ciphertexts and IVs for the same plaintext (Semantic Security)")
    void testUniqueIvPerEncryption() {
        String plaintext = "repeatable-secret";

        EncryptionService.EncryptionResult result1 = encryptionService.encrypt(plaintext);
        EncryptionService.EncryptionResult result2 = encryptionService.encrypt(plaintext);

        assertThat(result1.iv()).isNotEqualTo(result2.iv());
        assertThat(result1.encryptedValue()).isNotEqualTo(result2.encryptedValue());

        // Both decrypt to the same plaintext
        assertThat(encryptionService.decrypt(result1.encryptedValue(), result1.iv())).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(result2.encryptedValue(), result2.iv())).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Should throw exception when ciphertext is tampered with (GCM authentication check)")
    void testTamperedCiphertext() {
        String plaintext = "tamper-proof-secret";
        EncryptionService.EncryptionResult result = encryptionService.encrypt(plaintext);

        // Tamper with ciphertext by corrupting the base64 content
        byte[] decodedCipher = Base64.getDecoder().decode(result.encryptedValue());
        decodedCipher[0] ^= 0x5A; // flip bits
        String tamperedCipher = Base64.getEncoder().encodeToString(decodedCipher);

        assertThatThrownBy(() -> encryptionService.decrypt(tamperedCipher, result.iv()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tampering");
    }

    @Test
    @DisplayName("Should fail initialization if master key is not 32 bytes")
    void testInvalidKeyLength() {
        EncryptionService badService = new EncryptionService();
        String shortKey = Base64.getEncoder().encodeToString("short-key".getBytes());
        ReflectionTestUtils.setField(badService, "masterKeyBase64", shortKey);

        assertThatThrownBy(badService::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 32 bytes");
    }
}

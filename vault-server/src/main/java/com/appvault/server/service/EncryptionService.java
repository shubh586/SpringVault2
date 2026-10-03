package com.appvault.server.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption service using AES-256-GCM (Galois/Counter Mode).
 *
 * GCM provides both confidentiality AND integrity (authenticated encryption).
 * Each secret gets a unique IV (Initialization Vector), so even identical
 * plaintext values produce different ciphertexts.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes (96 bits, recommended for GCM)

    @Value("${vault.encryption.master-key}")
    private String masterKeyBase64;

    private SecretKeySpec secretKey;
    private SecureRandom secureRandom;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);

        // Require exact 32-byte key for AES-256 to avoid silent weakening/misconfiguration.
        if (keyBytes.length != 32) {
            throw new IllegalStateException("VAULT_MASTER_KEY must decode to exactly 32 bytes for AES-256-GCM");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
        log.info("✅ AES-256-GCM encryption initialized successfully");
    }

    /**
     * Encrypt plaintext and return Base64-encoded ciphertext along with the IV.
     *
     * @param plaintext The secret value to encrypt
     * @return EncryptionResult containing encrypted value and IV (both Base64 encoded)
     */
    public EncryptionResult encrypt(String plaintext) {
        try {
            // Generate a unique IV for this encryption
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptionResult(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt a Base64-encoded ciphertext using the provided IV.
     *
     * @param encryptedValueBase64 The encrypted value (Base64)
     * @param ivBase64            The IV used during encryption (Base64)
     * @return Decrypted plaintext string
     */
    public String decrypt(String encryptedValueBase64, String ivBase64) {
        try {
            byte[] ciphertext = Base64.getDecoder().decode(encryptedValueBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed - possible data tampering detected", e);
        }
    }

    /**
     * Holds the result of an encryption operation.
     */
    public record EncryptionResult(String encryptedValue, String iv) {}
}

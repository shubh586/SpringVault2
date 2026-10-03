package com.appvault.client.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialCacheTest {

    @Test
    @DisplayName("Should put and retrieve cached secret")
    void testPutAndGet() {
        CredentialCache cache = new CredentialCache(60, null);

        cache.put("database/credentials/password", "secret123");

        String cached = cache.get("database/credentials/password");
        assertThat(cached).isEqualTo("secret123");
    }

    @Test
    @DisplayName("Should return null for missing cache key")
    void testGetMissingKey() {
        CredentialCache cache = new CredentialCache(60, null);

        assertThat(cache.get("nonexistent/key")).isNull();
    }

    @Test
    @DisplayName("Should invalidate specific key")
    void testInvalidate() {
        CredentialCache cache = new CredentialCache(60, null);
        cache.put("api/stripe/key", "sk_live_123");

        cache.invalidate("api/stripe/key");

        assertThat(cache.get("api/stripe/key")).isNull();
    }

    @Test
    @DisplayName("Should clear all entries in cache")
    void testClear() {
        CredentialCache cache = new CredentialCache(60, null);
        cache.put("key1", "val1");
        cache.put("key2", "val2");

        cache.clear();

        assertThat(cache.get("key1")).isNull();
        assertThat(cache.get("key2")).isNull();
    }

    @Test
    @DisplayName("Should return null when entry is expired based on TTL")
    void testTtlExpiration() throws InterruptedException {
        // 1 second TTL
        CredentialCache cache = new CredentialCache(1, null);
        cache.put("temp/token", "temp-val");

        // Immediate fetch is present
        assertThat(cache.get("temp/token")).isEqualTo("temp-val");

        // Wait past TTL
        Thread.sleep(1100);

        assertThat(cache.get("temp/token")).isNull();
    }
}

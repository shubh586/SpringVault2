package com.appvault.client.cache;

import com.appvault.client.VaultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * In-memory credential cache with TTL expiration and background refresh.
 * 
 * - Secrets cached after first fetch (avoids repeated HTTP calls)
 * - TTL-based expiration (default 5 minutes)
 * - Background thread refreshes expiring entries
 * - Thread-safe using ConcurrentHashMap
 */
public class CredentialCache {

    private static final Logger log = LoggerFactory.getLogger(CredentialCache.class);

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int ttlSeconds;
    private final ScheduledExecutorService refreshScheduler;

    public CredentialCache(int ttlSeconds, VaultClient clientForRefresh) {
        this.ttlSeconds = ttlSeconds;

        if (clientForRefresh != null) {
            this.refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "vault-cache-refresh");
                t.setDaemon(true);
                return t;
            });

            // Schedule background refresh at 80% of TTL interval
            long refreshInterval = (long) (ttlSeconds * 0.8);
            this.refreshScheduler.scheduleAtFixedRate(
                    () -> refreshExpiring(clientForRefresh),
                    refreshInterval, refreshInterval, TimeUnit.SECONDS);

            log.info("Cache background refresh enabled (interval={}s, ttl={}s)",
                    refreshInterval, ttlSeconds);
        } else {
            this.refreshScheduler = null;
        }
    }

    /**
     * Get a cached value. Returns null if not present or expired.
     */
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;

        if (entry.isExpired(ttlSeconds)) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    /**
     * Cache a secret value.
     */
    public void put(String key, String value) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
    }

    /**
     * Remove a cached entry.
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /**
     * Clear the entire cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Background refresh for entries nearing expiration.
     */
    private void refreshExpiring(VaultClient client) {
        try {
            long now = System.currentTimeMillis();
            long expiryThreshold = (long) (ttlSeconds * 0.9) * 1000L;

            var keysToRefresh = new ArrayList<String>();
            cache.forEach((key, entry) -> {
                if (now - entry.createdAt > expiryThreshold) {
                    keysToRefresh.add(key);
                }
            });

            if (!keysToRefresh.isEmpty()) {
                log.debug("Refreshing {} expiring cache entries", keysToRefresh.size());
                Map<String, String> refreshed = client.batchGetSecrets(keysToRefresh);
                refreshed.forEach(this::put);
            }
        } catch (Exception e) {
            log.warn("Cache refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Internal cache entry with creation timestamp.
     */
    private static class CacheEntry {
        final String value;
        final long createdAt;

        CacheEntry(String value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }

        boolean isExpired(int ttlSeconds) {
            return System.currentTimeMillis() - createdAt > (long) ttlSeconds * 1000L;
        }
    }
}

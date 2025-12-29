package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Generic base cache for any entity type keyed by any key type.
 * <p>
 * Wraps {@link DomainCache} to provide a consistent, TTL-aware, thread-safe
 * caching interface usable across knk-core without Paper/Bukkit dependencies.
 *
 * @param <K> Key type
 * @param <V> Value/entity type
 */
public abstract class BaseCache<K, V> {

    protected final DomainCache<K, V> primary;

    /**
     * Create a cache with the specified TTL.
     *
     * @param ttl Time-to-live for cached entries. If null, defaults to 1 minute.
     */
    protected BaseCache(Duration ttl) {
        this.primary = new DomainCache<>(ttl);
    }

    /** Retrieve a value by key if present and not expired. */
    public Optional<V> get(K key) {
        return primary.get(key);
    }

    /** Retrieve a value by key even if expired (stale). */
    public Optional<V> getStale(K key) {
        return primary.getStale(key);
    }

    /** Store a single key/value pair. */
    public void put(K key, V value) {
        primary.put(key, value);
    }

    /** Batch store multiple key/value pairs. */
    public void putAll(Map<K, V> values) {
        primary.putAll(values);
    }

    /** Invalidate a single entry by key. */
    public void invalidate(K key) {
        primary.invalidate(key);
    }

    /** Clear all cached entries. */
    public void clear() {
        primary.clear();
    }

    /** Number of cached entries (including expired ones). */
    public int size() {
        return primary.size();
    }

    /** Access metrics for observability. */
    public DomainCache.CacheMetrics getMetrics() {
        return primary.getMetrics();
    }
}

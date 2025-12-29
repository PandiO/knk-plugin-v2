package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base cache for region-addressable domain entities.
 * <p>
 * Pattern encapsulated:
 * <ul>
 *   <li>Primary lookup by numeric ID</li>
 *   <li>Secondary lookup via WorldGuard region ID → numeric ID → entity</li>
 *   <li>Unified TTL and metrics via {@link DomainCache}</li>
 * </ul>
 * <p>
 * Subclasses provide mapping from entity to its primary ID and WorldGuard region ID
 * without introducing Paper/Bukkit dependencies in core.
 *
 * @param <V> Entity/detail type to cache
 */
public abstract class BaseRegionCache<V> {

    protected final DomainCache<Integer, V> byId;
    protected final DomainCache<String, Integer> wgRegionToId;

    /**
     * Create a cache with the specified TTL.
     *
     * @param ttl Time-to-live for cached entries. If null, defaults to 1 minute.
     */
    protected BaseRegionCache(Duration ttl) {
        this.byId = new DomainCache<>(ttl);
        this.wgRegionToId = new DomainCache<>(ttl);
    }

    /**
     * Retrieve an entity by its primary ID.
     */
    public Optional<V> getById(Integer id) {
        return byId.get(id);
    }

    /**
     * Retrieve an entity by its WorldGuard region ID.
     * Performs an indirect lookup: region ID → primary ID → entity.
     */
    public Optional<V> getByWgRegionId(String wgRegionId) {
        return wgRegionToId.get(wgRegionId).flatMap(byId::get);
    }

    /**
     * Store a single entity, indexing by both primary ID and region ID when present.
     */
    public void put(V value) {
        if (value == null) {
            return;
        }

        Integer id = getId(value);
        if (id != null) {
            byId.put(id, value);
        }

        String regionId = getWgRegionId(value);
        if (regionId != null && !regionId.isBlank() && id != null) {
            wgRegionToId.put(regionId, id);
        }
    }

    /**
     * Batch store multiple entities.
     */
    public void putAll(List<V> values) {
        if (values == null) {
            return;
        }
        for (V v : values) {
            put(v);
        }
    }

    /**
     * Invalidate an entity by its primary ID.
     * Note: region → ID mappings are left to expire via TTL.
     */
    public void invalidate(Integer id) {
        byId.invalidate(id);
    }

    /**
     * Clear all cached entries and mappings.
     */
    public void clear() {
        byId.clear();
        wgRegionToId.clear();
    }

    /**
     * Number of cached entities (by primary ID cache).
     */
    public int size() {
        return byId.size();
    }

    /**
     * Primary cache metrics (by-ID cache).
     */
    public DomainCache.CacheMetrics getMetrics() {
        return byId.getMetrics();
    }

    /**
     * Subclass must supply the primary ID for the entity.
     */
    protected abstract Integer getId(V value);

    /**
     * Subclass must supply the WorldGuard region ID for the entity, or null/blank if absent.
     */
    protected abstract String getWgRegionId(V value);
}

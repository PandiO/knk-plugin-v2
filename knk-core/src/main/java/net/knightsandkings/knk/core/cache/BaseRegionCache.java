package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base cache for region-addressable entities.
 * <p>
 * Builds on {@link BaseCache} to add a secondary index from WorldGuard region ID
 * to the primary numeric ID. Suitable for any entity that can be addressed by
 * both an integer ID and a region ID.
 *
 * @param <V> Entity/detail type to cache
 */
public abstract class BaseRegionCache<V> extends BaseCache<Integer, V> {

    protected final DomainCache<String, Integer> wgRegionToId;

    /**
     * Create a cache with the specified TTL.
     */
    protected BaseRegionCache(Duration ttl) {
        super(ttl);
        this.wgRegionToId = new DomainCache<>(ttl);
    }

    /** Retrieve an entity by its primary ID. */
    public Optional<V> getById(Integer id) {
        return primary.get(id);
    }

    /** Retrieve an entity by its WorldGuard region ID via indirect lookup. */
    public Optional<V> getByWgRegionId(String wgRegionId) {
        return wgRegionToId.get(wgRegionId).flatMap(primary::get);
    }

    /** Store a single entity with both indices when present. */
    public void put(V value) {
        if (value == null) {
            return;
        }
        Integer id = getId(value);
        if (id != null) {
            primary.put(id, value);
        }
        String regionId = getWgRegionId(value);
        if (regionId != null && !regionId.isBlank() && id != null) {
            wgRegionToId.put(regionId, id);
        }
    }

    /** Batch store multiple entities. */
    public void putAll(List<V> values) {
        if (values == null) {
            return;
        }
        for (V v : values) {
            put(v);
        }
    }

    /** Invalidate an entity by its primary ID. */
    public void invalidate(Integer id) {
        primary.invalidate(id);
    }

    /** Clear all cached entries and mappings. */
    @Override
    public void clear() {
        super.clear();
        wgRegionToId.clear();
    }

    /** Subclass must supply the primary ID for the entity. */
    protected abstract Integer getId(V value);

    /** Subclass must supply the WorldGuard region ID for the entity, or null/blank if absent. */
    protected abstract String getWgRegionId(V value);
}

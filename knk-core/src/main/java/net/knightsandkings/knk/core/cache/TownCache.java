package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.knightsandkings.knk.core.domain.towns.TownDetail;

/**
 * Type-safe cache for Town entities.
 * <p>
 * Provides multiple lookup strategies:
 * <ul>
 *   <li>By ID (primary key)</li>
 *   <li>By WorldGuard region ID</li>
 * </ul>
 * <p>
 * Both lookups share the same TTL and point to the same underlying TownDetail instances
 * to avoid duplication.
 */
public class TownCache {

    private final DomainCache<Integer, TownDetail> byId;
    private final DomainCache<String, Integer> wgRegionToId;  // Indirect lookup

    public TownCache(Duration ttl) {
        this.byId = new DomainCache<>(ttl);
        this.wgRegionToId = new DomainCache<>(ttl);
    }

    /**
     * Retrieves a town by its ID.
     *
     * @param id The town ID
     * @return Optional containing the town if cached and not expired
     */
    public Optional<TownDetail> getById(Integer id) {
        return byId.get(id);
    }

    /**
     * Retrieves a town by its WorldGuard region ID.
     * <p>
     * This performs an indirect lookup: region ID → town ID → TownDetail.
     *
     * @param wgRegionId The WorldGuard region ID
     * @return Optional containing the town if cached and not expired
     */
    public Optional<TownDetail> getByWgRegionId(String wgRegionId) {
        return wgRegionToId.get(wgRegionId)
            .flatMap(byId::get);
    }

    /**
     * Stores a town in the cache, indexing by both ID and WorldGuard region ID.
     *
     * @param town The town to cache
     */
    public void put(TownDetail town) {
        if (town == null) {
            return;
        }

        byId.put(town.id(), town);

        // Index by WorldGuard region ID if available
        if (town.wgRegionId() != null && !town.wgRegionId().isBlank()) {
            wgRegionToId.put(town.wgRegionId(), town.id());
        }
    }

    /**
     * Batch stores multiple towns (e.g., from a search API response).
     *
     * @param towns List of towns to cache
     */
    public void putAll(List<TownDetail> towns) {
        if (towns == null) {
            return;
        }

        for (TownDetail town : towns) {
            put(town);
        }
    }

    /**
     * Invalidates a town from the cache by ID.
     * <p>
     * Note: This does NOT remove the WorldGuard region → ID mapping. In practice,
     * region IDs rarely change, and stale mappings will naturally expire via TTL.
     *
     * @param id The town ID to invalidate
     */
    public void invalidate(Integer id) {
        byId.invalidate(id);
    }

    /**
     * Clears the entire cache (both by-ID and region ID mappings).
     */
    public void clear() {
        byId.clear();
        wgRegionToId.clear();
    }

    /**
     * Returns the number of towns currently cached.
     *
     * @return Cache size
     */
    public int size() {
        return byId.size();
    }

    /**
     * Returns cache metrics for the by-ID cache (primary metrics).
     *
     * @return Cache metrics
     */
    public DomainCache.CacheMetrics getMetrics() {
        return byId.getMetrics();
    }
}

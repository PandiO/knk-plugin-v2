package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.knightsandkings.knk.core.domain.structures.StructureDetail;

/**
 * Type-safe cache for Structure entities.
 * <p>
 * Provides multiple lookup strategies:
 * <ul>
 *   <li>By ID (primary key)</li>
 *   <li>By WorldGuard region ID</li>
 * </ul>
 */
public class StructureCache {

    private final DomainCache<Integer, StructureDetail> byId;
    private final DomainCache<String, Integer> wgRegionToId;  // Indirect lookup

    public StructureCache(Duration ttl) {
        this.byId = new DomainCache<>(ttl);
        this.wgRegionToId = new DomainCache<>(ttl);
    }

    /**
     * Retrieves a structure by its ID.
     *
     * @param id The structure ID
     * @return Optional containing the structure if cached and not expired
     */
    public Optional<StructureDetail> getById(Integer id) {
        return byId.get(id);
    }

    /**
     * Retrieves a structure by its WorldGuard region ID.
     *
     * @param wgRegionId The WorldGuard region ID
     * @return Optional containing the structure if cached and not expired
     */
    public Optional<StructureDetail> getByWgRegionId(String wgRegionId) {
        return wgRegionToId.get(wgRegionId)
            .flatMap(byId::get);
    }

    /**
     * Stores a structure in the cache.
     *
     * @param structure The structure to cache
     */
    public void put(StructureDetail structure) {
        if (structure == null) {
            return;
        }

        byId.put(structure.id(), structure);

        if (structure.wgRegionId() != null && !structure.wgRegionId().isBlank()) {
            wgRegionToId.put(structure.wgRegionId(), structure.id());
        }
    }

    /**
     * Batch stores multiple structures.
     *
     * @param structures List of structures to cache
     */
    public void putAll(List<StructureDetail> structures) {
        if (structures == null) {
            return;
        }

        for (StructureDetail structure : structures) {
            put(structure);
        }
    }

    /**
     * Invalidates a structure from the cache by ID.
     *
     * @param id The structure ID to invalidate
     */
    public void invalidate(Integer id) {
        byId.invalidate(id);
    }

    /**
     * Clears the entire cache.
     */
    public void clear() {
        byId.clear();
        wgRegionToId.clear();
    }

    /**
     * Returns the number of structures currently cached.
     *
     * @return Cache size
     */
    public int size() {
        return byId.size();
    }

    /**
     * Returns cache metrics.
     *
     * @return Cache metrics
     */
    public DomainCache.CacheMetrics getMetrics() {
        return byId.getMetrics();
    }
}

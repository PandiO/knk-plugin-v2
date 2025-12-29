package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail;

/**
 * Type-safe cache for District entities.
 * <p>
 * Provides multiple lookup strategies:
 * <ul>
 *   <li>By ID (primary key)</li>
 *   <li>By WorldGuard region ID</li>
 * </ul>
 */
public class DistrictCache {

    private final DomainCache<Integer, DistrictDetail> byId;
    private final DomainCache<String, Integer> wgRegionToId;  // Indirect lookup

    public DistrictCache(Duration ttl) {
        this.byId = new DomainCache<>(ttl);
        this.wgRegionToId = new DomainCache<>(ttl);
    }

    /**
     * Retrieves a district by its ID.
     *
     * @param id The district ID
     * @return Optional containing the district if cached and not expired
     */
    public Optional<DistrictDetail> getById(Integer id) {
        return byId.get(id);
    }

    /**
     * Retrieves a district by its WorldGuard region ID.
     *
     * @param wgRegionId The WorldGuard region ID
     * @return Optional containing the district if cached and not expired
     */
    public Optional<DistrictDetail> getByWgRegionId(String wgRegionId) {
        return wgRegionToId.get(wgRegionId)
            .flatMap(byId::get);
    }

    /**
     * Stores a district in the cache.
     *
     * @param district The district to cache
     */
    public void put(DistrictDetail district) {
        if (district == null) {
            return;
        }

        byId.put(district.id(), district);

        if (district.wgRegionId() != null && !district.wgRegionId().isBlank()) {
            wgRegionToId.put(district.wgRegionId(), district.id());
        }
    }

    /**
     * Batch stores multiple districts.
     *
     * @param districts List of districts to cache
     */
    public void putAll(List<DistrictDetail> districts) {
        if (districts == null) {
            return;
        }

        for (DistrictDetail district : districts) {
            put(district);
        }
    }

    /**
     * Invalidates a district from the cache by ID.
     *
     * @param id The district ID to invalidate
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
     * Returns the number of districts currently cached.
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

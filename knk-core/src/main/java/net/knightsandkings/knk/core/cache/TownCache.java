package net.knightsandkings.knk.core.cache;

import java.time.Duration;
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
public class TownCache extends BaseRegionCache<TownDetail> {

    public TownCache(Duration ttl) {
        super(ttl);
    }

    @Override
    protected Integer getId(TownDetail value) {
        return value.id();
    }

    @Override
    protected String getWgRegionId(TownDetail value) {
        return value.wgRegionId();
    }
}

package net.knightsandkings.knk.core.cache;

import java.time.Duration;
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
public class StructureCache extends BaseRegionCache<StructureDetail> {

    public StructureCache(Duration ttl) {
        super(ttl);
    }

    @Override
    protected Integer getId(StructureDetail value) {
        return value.id();
    }

    @Override
    protected String getWgRegionId(StructureDetail value) {
        return value.wgRegionId();
    }
}

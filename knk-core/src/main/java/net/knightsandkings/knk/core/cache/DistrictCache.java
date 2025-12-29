package net.knightsandkings.knk.core.cache;

import java.time.Duration;
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
public class DistrictCache extends BaseRegionCache<DistrictDetail> {

    public DistrictCache(Duration ttl) {
        super(ttl);
    }

    @Override
    protected Integer getId(DistrictDetail value) {
        return value.id();
    }

    @Override
    protected String getWgRegionId(DistrictDetail value) {
        return value.wgRegionId();
    }
}

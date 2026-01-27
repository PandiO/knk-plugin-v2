package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import net.knightsandkings.knk.core.domain.streets.StreetDetail;

/**
 * Type-safe cache for Street entities.
 * <p>
 * Provides lookup by ID (primary key).
 * Streets do not have WorldGuard regions, so only ID lookup is supported.
 */
public class StreetCache extends BaseCache<Integer, StreetDetail> {

    public StreetCache(Duration ttl) {
        super(ttl);
    }
    
    /**
     * Store a street in cache using its ID as the key.
     *
     * @param street The street to cache
     */
    public void put(StreetDetail street) {
        if (street != null && street.id() != null) {
            put(street.id(), street);
        }
    }
}

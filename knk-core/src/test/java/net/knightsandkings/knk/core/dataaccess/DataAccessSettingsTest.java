package net.knightsandkings.knk.core.dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class DataAccessSettingsTest {
    
    @Test
    void resolvePolicyUsesDefaultWhenNull() {
        DataAccessSettings settings = new DataAccessSettings(FetchPolicy.CACHE_FIRST, true, RetryPolicy.defaultPolicy());
        assertEquals(FetchPolicy.CACHE_FIRST, settings.resolvePolicy(null));
    }
    
    @Test
    void resolvePolicyKeepsRequestedWhenProvided() {
        DataAccessSettings settings = new DataAccessSettings(FetchPolicy.CACHE_FIRST, true, RetryPolicy.defaultPolicy());
        assertEquals(FetchPolicy.API_ONLY, settings.resolvePolicy(FetchPolicy.API_ONLY));
    }
    
    @Test
    void staleDisabledDowngradesStaleOkToCacheFirst() {
        DataAccessSettings settings = new DataAccessSettings(FetchPolicy.CACHE_FIRST, false, RetryPolicy.defaultPolicy());
        assertEquals(FetchPolicy.CACHE_FIRST, settings.resolvePolicy(FetchPolicy.STALE_OK));
    }
}

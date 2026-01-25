package net.knightsandkings.knk.core.dataaccess;

/**
 * Defines the data retrieval strategy for cache/API interactions.
 * <p>
 * Controls where data is fetched from (cache, API, or both) and in what order,
 * providing flexibility for different use cases from high-performance cached reads
 * to guaranteed fresh API data.
 */
public enum FetchPolicy {
    
    /**
     * Only check cache; never hit API.
     * <p>
     * Use when offline mode or when API is known to be unavailable.
     * Returns NOT_FOUND if cache miss occurs.
     */
    CACHE_ONLY,
    
    /**
     * Check cache first; on miss, fetch from API and write-through to cache.
     * <p>
     * Default policy for most use cases. Provides optimal performance while
     * ensuring data availability via API fallback.
     */
    CACHE_FIRST,
    
    /**
     * Always fetch from API; ignore cache for read, but write-through on success.
     * <p>
     * Use when guaranteed fresh data is required (e.g., admin commands, critical updates).
     * Cache is still updated to benefit subsequent CACHE_FIRST requests.
     */
    API_ONLY,
    
    /**
     * Fetch from API first; on failure, try cache as fallback.
     * <p>
     * Inverse of CACHE_FIRST. Ensures fresh data when API is healthy,
     * but degrades gracefully to cached data on API failure.
     */
    API_THEN_CACHE_REFRESH,
    
    /**
     * Check cache first (even if stale); on API failure, return stale data if available.
     * <p>
     * Most resilient option. Allows serving potentially outdated data
     * rather than failing completely. Useful for non-critical reads during
     * API outages. Background refresh may be triggered after serving stale data.
     */
    STALE_OK
}

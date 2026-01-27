package net.knightsandkings.knk.core.dataaccess;

/**
 * Outcome status of a fetch operation.
 * <p>
 * Indicates what happened during the data retrieval attempt,
 * enabling callers to make informed decisions based on the result.
 */
public enum FetchStatus {
    
    /**
     * Data was found in cache and returned without API call.
     */
    HIT,
    
    /**
     * Cache miss occurred; data was fetched from API and cached.
     */
    MISS_FETCHED,
    
    /**
     * Entity does not exist (cache miss + API returned 404).
     */
    NOT_FOUND,
    
    /**
     * An error occurred during fetch (API failure, timeout, etc.).
     * Check {@link FetchResult#error()} for details.
     */
    ERROR,
    
    /**
     * Fresh data unavailable, but stale cached data was returned.
     * Only occurs when using {@link FetchPolicy#STALE_OK}.
     */
    STALE_SERVED
}

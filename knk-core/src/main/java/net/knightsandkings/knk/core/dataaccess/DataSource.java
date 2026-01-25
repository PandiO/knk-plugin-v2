package net.knightsandkings.knk.core.dataaccess;

/**
 * Indicates where the fetched data originated from.
 * <p>
 * Useful for observability, debugging, and understanding
 * cache effectiveness.
 */
public enum DataSource {
    
    /**
     * Data was retrieved from the in-memory cache.
     */
    CACHE,
    
    /**
     * Data was retrieved from the Web API.
     */
    API,
    
    /**
     * Data source is unknown or indeterminate (e.g., on error).
     */
    UNKNOWN
}

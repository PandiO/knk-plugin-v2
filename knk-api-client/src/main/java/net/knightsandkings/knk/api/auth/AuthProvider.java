package net.knightsandkings.knk.api.auth;

/**
 * Authentication provider for API requests.
 */
public interface AuthProvider {
    /**
     * Get authentication header value (e.g., "Bearer token" or "ApiKey xyz").
     * @return auth header value, or null if no auth
     */
    String getAuthHeader();
    
    /**
     * Get authentication header name (e.g., "Authorization" or "X-API-Key").
     * @return header name, or null if no auth
     */
    String getAuthHeaderName();
}

package net.knightsandkings.knk.api.auth;

/**
 * API key authentication provider.
 */
public class ApiKeyAuthProvider implements AuthProvider {
    private final String apiKey;
    private final String headerName;
    
    public ApiKeyAuthProvider(String apiKey) {
        this(apiKey, "X-API-Key");
    }
    
    public ApiKeyAuthProvider(String apiKey, String headerName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        if (headerName == null || headerName.isBlank()) {
            throw new IllegalArgumentException("Header name cannot be null or blank");
        }
        this.apiKey = apiKey;
        this.headerName = headerName;
    }
    
    @Override
    public String getAuthHeader() {
        return apiKey;
    }
    
    @Override
    public String getAuthHeaderName() {
        return headerName;
    }
}

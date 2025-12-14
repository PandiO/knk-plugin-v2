package net.knightsandkings.knk.api.auth;

/**
 * Bearer token authentication provider.
 */
public class BearerAuthProvider implements AuthProvider {
    private final String token;
    
    public BearerAuthProvider(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token cannot be null or blank");
        }
        this.token = token;
    }
    
    @Override
    public String getAuthHeader() {
        return "Bearer " + token;
    }
    
    @Override
    public String getAuthHeaderName() {
        return "Authorization";
    }
}

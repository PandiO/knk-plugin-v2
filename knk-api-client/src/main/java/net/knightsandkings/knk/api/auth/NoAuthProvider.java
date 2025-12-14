package net.knightsandkings.knk.api.auth;

/**
 * No authentication provider.
 */
public class NoAuthProvider implements AuthProvider {
    @Override
    public String getAuthHeader() {
        return null;
    }
    
    @Override
    public String getAuthHeaderName() {
        return null;
    }
}

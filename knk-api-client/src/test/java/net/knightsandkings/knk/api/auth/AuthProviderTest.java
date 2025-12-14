package net.knightsandkings.knk.api.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthProviderTest {
    
    @Test
    void noAuthProviderShouldReturnNull() {
        AuthProvider provider = new NoAuthProvider();
        assertNull(provider.getAuthHeader());
        assertNull(provider.getAuthHeaderName());
    }
    
    @Test
    void bearerAuthProviderShouldFormatToken() {
        AuthProvider provider = new BearerAuthProvider("my-secret-token");
        assertEquals("Bearer my-secret-token", provider.getAuthHeader());
        assertEquals("Authorization", provider.getAuthHeaderName());
    }
    
    @Test
    void bearerAuthProviderShouldRejectNullToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BearerAuthProvider(null);
        });
    }
    
    @Test
    void apiKeyAuthProviderShouldUseDefaultHeader() {
        AuthProvider provider = new ApiKeyAuthProvider("my-api-key");
        assertEquals("my-api-key", provider.getAuthHeader());
        assertEquals("X-API-Key", provider.getAuthHeaderName());
    }
    
    @Test
    void apiKeyAuthProviderShouldUseCustomHeader() {
        AuthProvider provider = new ApiKeyAuthProvider("my-api-key", "X-Custom-Key");
        assertEquals("my-api-key", provider.getAuthHeader());
        assertEquals("X-Custom-Key", provider.getAuthHeaderName());
    }
    
    @Test
    void apiKeyAuthProviderShouldRejectNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ApiKeyAuthProvider(null);
        });
    }
}

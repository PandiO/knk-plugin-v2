package net.knightsandkings.knk.paper.config;

import java.time.Duration;

/**
 * Plugin configuration loaded from config.yml.
 */
public record KnkConfig(
    ApiConfig api
) {
    public record ApiConfig(
        String baseUrl,
        boolean debugLogging,
        boolean allowUntrustedSsl,
        AuthConfig auth,
        TimeoutsConfig timeouts
    ) {
        public void validate() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("api.base-url is required");
            }
            if (auth == null) {
                throw new IllegalArgumentException("api.auth is required");
            }
            auth.validate();
            if (timeouts == null) {
                throw new IllegalArgumentException("api.timeouts is required");
            }
        }
    }
    
    public record AuthConfig(
        String type,
        String bearerToken,
        String apiKey,
        String apiKeyHeader
    ) {
        public void validate() {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("api.auth.type is required (none, bearer, apikey)");
            }
            String lowerType = type.toLowerCase();
            if (!lowerType.equals("none") && !lowerType.equals("bearer") && !lowerType.equals("apikey")) {
                throw new IllegalArgumentException(
                    "api.auth.type must be one of: none, bearer, apikey (got: " + type + ")"
                );
            }
            if (lowerType.equals("bearer") && (bearerToken == null || bearerToken.isBlank())) {
                throw new IllegalArgumentException("api.auth.bearer-token is required when type=bearer");
            }
            if (lowerType.equals("apikey") && (apiKey == null || apiKey.isBlank())) {
                throw new IllegalArgumentException("api.auth.api-key is required when type=apikey");
            }
        }
    }
    
    public record TimeoutsConfig(
        int connect,
        int read,
        int write
    ) {
        public Duration connectDuration() {
            return Duration.ofSeconds(connect);
        }
        
        public Duration readDuration() {
            return Duration.ofSeconds(read);
        }
        
        public Duration writeDuration() {
            return Duration.ofSeconds(write);
        }
    }
    
    public void validate() {
        if (api == null) {
            throw new IllegalArgumentException("api configuration is required");
        }
        api.validate();
    }
}

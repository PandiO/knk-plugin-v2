package net.knightsandkings.knk.paper.config;

import java.time.Duration;

/**
 * Plugin configuration loaded from config.yml.
 */
public record KnkConfig(
    ApiConfig api,
    CacheConfig cache
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
        if (cache == null) {
            throw new IllegalArgumentException("cache configuration is required");
        }
    }
    
    public record CacheConfig(
        int ttlSeconds,
        EntityCacheSettings entities
    ) {
        /**
         * Returns the cache TTL as a Duration.
         *
         * @return Cache TTL duration
         */
        public Duration ttl() {
            return Duration.ofSeconds(ttlSeconds);
        }
        
        /**
         * Returns a default cache configuration.
         *
         * @return Default CacheConfig with 60 second TTL
         */
        public static CacheConfig defaultConfig() {
            return new CacheConfig(60, EntityCacheSettings.defaults());
        }
    }

    public record EntityCacheSettings(
        EntitySettings users,
        EntitySettings towns,
        EntitySettings districts,
        EntitySettings structures,
        EntitySettings streets,
        EntitySettings locations,
        EntitySettings enchantments,
        EntitySettings itemBlueprints,
        EntitySettings minecraftMaterials,
        EntitySettings domains,
        EntitySettings health
    ) {
        public static EntityCacheSettings defaults() {
            return new EntityCacheSettings(
                EntitySettings.defaults(), // users
                EntitySettings.defaults(), // towns
                EntitySettings.defaults(), // districts
                EntitySettings.defaults(), // structures
                EntitySettings.defaults(), // streets
                EntitySettings.defaults(), // locations
                EntitySettings.defaults(), // enchantments
                EntitySettings.defaults(), // itemBlueprints
                EntitySettings.defaults(), // minecraftMaterials
                EntitySettings.defaults(), // domains
                EntitySettings.defaults()  // health
            );
        }
    }

    public record EntitySettings(
        Integer ttlMinutes,
        Integer ttlSeconds,
        Integer maxTtlMinutes,
        Integer maxTtlSeconds,
        String defaultPolicy,
        Boolean allowStale,
        Integer retryAttempts,
        Integer retryBackoffMs
    ) {
        public Duration ttl() {
            if (ttlSeconds != null) {
                return Duration.ofSeconds(ttlSeconds);
            }
            if (ttlMinutes != null) {
                return Duration.ofMinutes(ttlMinutes);
            }
            return Duration.ofMinutes(15); // fallback default
        }

        public Duration maxTtl() {
            if (maxTtlSeconds != null) {
                return Duration.ofSeconds(maxTtlSeconds);
            }
            if (maxTtlMinutes != null) {
                return Duration.ofMinutes(maxTtlMinutes);
            }
            return Duration.ofHours(1); // fallback default
        }

        public String policyName() {
            return defaultPolicy != null ? defaultPolicy : "CACHE_FIRST";
        }

        public boolean isStaleAllowed() {
            return allowStale != null ? allowStale : true;
        }

        public int maxRetries() {
            return retryAttempts != null ? retryAttempts : 3;
        }

        public int backoffMs() {
            return retryBackoffMs != null ? retryBackoffMs : 100;
        }

        public static EntitySettings defaults() {
            return new EntitySettings(
                15, // ttlMinutes
                null, // ttlSeconds
                60, // maxTtlMinutes
                null, // maxTtlSeconds
                "CACHE_FIRST", // defaultPolicy
                true, // allowStale
                3, // retryAttempts
                100  // retryBackoffMs
            );
        }
    }
}

package net.knightsandkings.knk.paper.config;

import java.time.Duration;

/**
 * Plugin configuration loaded from config.yml.
 */
public record KnkConfig(
    ApiConfig api,
    CacheConfig cache,
    AccountConfig account,
    MessagesConfig messages
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
        if (account == null) {
            throw new IllegalArgumentException("account configuration is required");
        }
        account.validate();
        if (messages == null) {
            throw new IllegalArgumentException("messages configuration is required");
        }
        messages.validate();
    }
    
    public record CacheConfig(
        int ttlSeconds
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
            return new CacheConfig(60);
        }
    }
    
    /**
     * Account management configuration (Phase 2+).
     * Controls account linking, chat capture timeouts, and related settings.
     */
    public record AccountConfig(
        int linkCodeExpiryMinutes,
        int chatCaptureTimeoutSeconds
    ) {
        /**
         * Validate account configuration values.
         * Ensures link code expiry and chat timeout are within acceptable ranges.
         */
        public void validate() {
            if (linkCodeExpiryMinutes < 1) {
                throw new IllegalArgumentException(
                    "account.link-code-expiry-minutes must be at least 1 (got: " + linkCodeExpiryMinutes + ")"
                );
            }
            if (linkCodeExpiryMinutes > 120) {
                throw new IllegalArgumentException(
                    "account.link-code-expiry-minutes must not exceed 120 (2 hours) (got: " + linkCodeExpiryMinutes + ")"
                );
            }
            if (chatCaptureTimeoutSeconds < 30) {
                throw new IllegalArgumentException(
                    "account.chat-capture-timeout-seconds must be at least 30 (got: " + chatCaptureTimeoutSeconds + ")"
                );
            }
            if (chatCaptureTimeoutSeconds > 300) {
                throw new IllegalArgumentException(
                    "account.chat-capture-timeout-seconds must not exceed 300 (5 minutes) (got: " + chatCaptureTimeoutSeconds + ")"
                );
            }
        }
        
        /**
         * Get link code expiry as a Duration.
         */
        public Duration linkCodeExpiry() {
            return Duration.ofMinutes(linkCodeExpiryMinutes);
        }
        
        /**
         * Get chat capture timeout as a Duration.
         */
        public Duration chatCaptureTimeout() {
            return Duration.ofSeconds(chatCaptureTimeoutSeconds);
        }
    }
    
    /**
     * Messages configuration for player-facing text.
     * All messages support Minecraft color codes (&a, &6, etc.).
     * Placeholders: {code}, {minutes}, {coins}, {gems}, {exp}
     */
    public record MessagesConfig(
        String prefix,
        String accountCreated,
        String accountLinked,
        String linkCodeGenerated,
        String invalidLinkCode,
        String duplicateAccount,
        String mergeComplete
    ) {
        /**
         * Validate messages configuration.
         * Ensures all required messages are present.
         */
        public void validate() {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("messages.prefix is required");
            }
            if (accountCreated == null || accountCreated.isBlank()) {
                throw new IllegalArgumentException("messages.account-created is required");
            }
            if (accountLinked == null || accountLinked.isBlank()) {
                throw new IllegalArgumentException("messages.account-linked is required");
            }
            if (linkCodeGenerated == null || linkCodeGenerated.isBlank()) {
                throw new IllegalArgumentException("messages.link-code-generated is required");
            }
            if (invalidLinkCode == null || invalidLinkCode.isBlank()) {
                throw new IllegalArgumentException("messages.invalid-link-code is required");
            }
            if (duplicateAccount == null || duplicateAccount.isBlank()) {
                throw new IllegalArgumentException("messages.duplicate-account is required");
            }
            if (mergeComplete == null || mergeComplete.isBlank()) {
                throw new IllegalArgumentException("messages.merge-complete is required");
            }
        }
    }
}

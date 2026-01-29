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
        if (account != null) {
            account.validate();
        }
    }
    
    public record AccountConfig(
        int linkCodeExpiryMinutes,
        int chatCaptureTimeoutSeconds
    ) {
        public void validate() {
            if (linkCodeExpiryMinutes <= 0) {
                throw new IllegalArgumentException("account.link-code-expiry-minutes must be positive");
            }
            if (chatCaptureTimeoutSeconds <= 0) {
                throw new IllegalArgumentException("account.chat-capture-timeout-seconds must be positive");
            }
        }
        
        /**
         * Returns default account configuration.
         */
        public static AccountConfig defaultConfig() {
            return new AccountConfig(20, 120);
        }
    }
    
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
         * Format a message by replacing placeholders.
         */
        public String format(String template, Object... args) {
            String result = template;
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    String placeholder = "{" + args[i] + "}";
                    String value = String.valueOf(args[i + 1]);
                    result = result.replace(placeholder, value);
                }
            }
            return result;
        }
        
        /**
         * Returns default messages configuration.
         */
        public static MessagesConfig defaultConfig() {
            return new MessagesConfig(
                "&8[&6KnK&8] &r",
                "&aAccount created successfully! You can now log in on the web app.",
                "&aYour accounts have been linked!",
                "&aYour link code is: &6{code}&a. Use this code in the web app. Expires in {minutes} minutes.",
                "&cThis code is invalid or has expired. Use &6/account link &cto get a new one.",
                "&cYou have two accounts. Please choose which one to keep.",
                "&aAccount merge complete. Your account now has {coins} coins, {gems} gems, and {exp} XP."
            );
        }
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
}

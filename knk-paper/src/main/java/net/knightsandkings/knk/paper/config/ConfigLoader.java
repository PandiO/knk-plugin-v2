package net.knightsandkings.knk.paper.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Loads and parses plugin configuration from config.yml.
 */
public class ConfigLoader {
    
    public static KnkConfig load(FileConfiguration config) {
        ConfigurationSection apiSection = config.getConfigurationSection("api");
        if (apiSection == null) {
            throw new IllegalArgumentException("Missing 'api' section in config.yml");
        }
        
        String baseUrl = apiSection.getString("base-url");
        boolean debugLogging = apiSection.getBoolean("debug-logging", false);
        boolean allowUntrustedSsl = apiSection.getBoolean("allow-untrusted-ssl", false);
        
        ConfigurationSection authSection = apiSection.getConfigurationSection("auth");
        if (authSection == null) {
            throw new IllegalArgumentException("Missing 'api.auth' section in config.yml");
        }
        
        KnkConfig.AuthConfig auth = new KnkConfig.AuthConfig(
            authSection.getString("type", "none"),
            authSection.getString("bearer-token", ""),
            authSection.getString("api-key", ""),
            authSection.getString("api-key-header", "X-API-Key")
        );
        
        ConfigurationSection timeoutsSection = apiSection.getConfigurationSection("timeouts");
        if (timeoutsSection == null) {
            throw new IllegalArgumentException("Missing 'api.timeouts' section in config.yml");
        }
        
        KnkConfig.TimeoutsConfig timeouts = new KnkConfig.TimeoutsConfig(
            timeoutsSection.getInt("connect", 10),
            timeoutsSection.getInt("read", 10),
            timeoutsSection.getInt("write", 10)
        );
        
        KnkConfig.ApiConfig apiConfig = new KnkConfig.ApiConfig(baseUrl, debugLogging, allowUntrustedSsl, auth, timeouts);
        
        // Load cache configuration
        ConfigurationSection cacheSection = config.getConfigurationSection("cache");
        KnkConfig.CacheConfig cacheConfig;
        if (cacheSection != null) {
            cacheConfig = new KnkConfig.CacheConfig(
                cacheSection.getInt("ttl-seconds", 60)
            );
        } else {
            // Use defaults if cache section is missing
            cacheConfig = KnkConfig.CacheConfig.defaultConfig();
        }
        
        // Load account configuration
        ConfigurationSection accountSection = config.getConfigurationSection("account");
        KnkConfig.AccountConfig accountConfig;
        if (accountSection != null) {
            accountConfig = new KnkConfig.AccountConfig(
                accountSection.getInt("link-code-expiry-minutes", 20),
                accountSection.getInt("chat-capture-timeout-seconds", 120)
            );
        } else {
            // Use defaults if account section is missing
            accountConfig = KnkConfig.AccountConfig.defaultConfig();
        }
        
        // Load messages configuration
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        KnkConfig.MessagesConfig messagesConfig;
        if (messagesSection != null) {
            messagesConfig = new KnkConfig.MessagesConfig(
                messagesSection.getString("prefix", "&8[&6KnK&8] &r"),
                messagesSection.getString("account-created", "&aAccount created successfully!"),
                messagesSection.getString("account-linked", "&aYour accounts have been linked!"),
                messagesSection.getString("link-code-generated", "&aYour link code is: &6{code}&a."),
                messagesSection.getString("invalid-link-code", "&cThis code is invalid or has expired."),
                messagesSection.getString("duplicate-account", "&cYou have two accounts."),
                messagesSection.getString("merge-complete", "&aAccount merge complete.")
            );
        } else {
            // Use defaults if messages section is missing
            messagesConfig = KnkConfig.MessagesConfig.defaultConfig();
        }
        
        KnkConfig knkConfig = new KnkConfig(apiConfig, cacheConfig, accountConfig, messagesConfig);
        knkConfig.validate();
        
        return knkConfig;
    }
}

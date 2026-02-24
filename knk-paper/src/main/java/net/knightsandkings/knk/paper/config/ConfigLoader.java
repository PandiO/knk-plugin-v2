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
            int ttlSeconds = cacheSection.getInt("ttl-seconds", 60);
            
            // Load entity-specific settings
            KnkConfig.EntityCacheSettings entitySettings = loadEntityCacheSettings(cacheSection);
            
            cacheConfig = new KnkConfig.CacheConfig(ttlSeconds, entitySettings);
        } else {
            // Use defaults if cache section is missing
            cacheConfig = KnkConfig.CacheConfig.defaultConfig();
        }
        
        KnkConfig knkConfig = new KnkConfig(apiConfig, cacheConfig);
        knkConfig.validate();
        
        return knkConfig;
    }
    
    private static KnkConfig.EntityCacheSettings loadEntityCacheSettings(ConfigurationSection cacheSection) {
        ConfigurationSection entitiesSection = cacheSection.getConfigurationSection("entities");
        if (entitiesSection == null) {
            return KnkConfig.EntityCacheSettings.defaults();
        }
        
        return new KnkConfig.EntityCacheSettings(
            loadEntitySettings(entitiesSection, "users"),
            loadEntitySettings(entitiesSection, "towns"),
            loadEntitySettings(entitiesSection, "districts"),
            loadEntitySettings(entitiesSection, "structures"),
            loadEntitySettings(entitiesSection, "streets"),
            loadEntitySettings(entitiesSection, "locations"),
            loadEntitySettings(entitiesSection, "enchantments"),
            loadEntitySettings(entitiesSection, "itemBlueprints"),
            loadEntitySettings(entitiesSection, "minecraftMaterials"),
            loadEntitySettings(entitiesSection, "domains"),
            loadEntitySettings(entitiesSection, "health")
        );
    }
    
    private static KnkConfig.EntitySettings loadEntitySettings(ConfigurationSection entitiesSection, String entityName) {
        ConfigurationSection section = entitiesSection.getConfigurationSection(entityName);
        if (section == null) {
            return KnkConfig.EntitySettings.defaults();
        }
        
        Integer ttlMinutes = section.contains("ttl-minutes") ? section.getInt("ttl-minutes") : null;
        Integer ttlSeconds = section.contains("ttl-seconds") ? section.getInt("ttl-seconds") : null;
        Integer maxTtlMinutes = section.contains("max-ttl-minutes") ? section.getInt("max-ttl-minutes") : null;
        Integer maxTtlSeconds = section.contains("max-ttl-seconds") ? section.getInt("max-ttl-seconds") : null;
        String defaultPolicy = section.getString("default-policy");
        Boolean allowStale = section.contains("allow-stale") ? section.getBoolean("allow-stale") : null;
        Integer retryAttempts = section.contains("retry-attempts") ? section.getInt("retry-attempts") : null;
        Integer retryBackoffMs = section.contains("retry-backoff-ms") ? section.getInt("retry-backoff-ms") : null;
        
        return new KnkConfig.EntitySettings(
            ttlMinutes,
            ttlSeconds,
            maxTtlMinutes,
            maxTtlSeconds,
            defaultPolicy,
            allowStale,
            retryAttempts,
            retryBackoffMs
        );
    }
}

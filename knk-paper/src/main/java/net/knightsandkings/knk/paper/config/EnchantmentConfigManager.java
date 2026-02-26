package net.knightsandkings.knk.paper.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class EnchantmentConfigManager {
    private static final String ROOT = "custom-enchantments";
    private static final String DEFAULT_COOLDOWN_MESSAGE = "&c%seconds% seconds remaining";

    private final Plugin plugin;

    public EnchantmentConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean disableForCreative() {
        FileConfiguration config = plugin.getConfig();
        if (config == null) {
            return false;
        }
        return config.getBoolean(ROOT + ".disable-for-creative", false);
    }

    public String cooldownMessageTemplate() {
        FileConfiguration config = plugin.getConfig();
        if (config == null) {
            return DEFAULT_COOLDOWN_MESSAGE;
        }

        String configured = config.getString(ROOT + ".cooldown-message", DEFAULT_COOLDOWN_MESSAGE);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_COOLDOWN_MESSAGE;
        }
        return configured;
    }

    public String getMessage(String key, String fallback) {
        FileConfiguration config = plugin.getConfig();
        if (config == null) {
            return fallback;
        }
        return config.getString(ROOT + "." + key, fallback);
    }

    public String getMessage(String key, String fallback, Map<String, String> placeholders) {
        String value = getMessage(key, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue());
        }
        return value;
    }

    public void reload() {
        plugin.reloadConfig();
    }
}
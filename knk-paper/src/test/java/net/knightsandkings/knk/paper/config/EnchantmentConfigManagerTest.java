package net.knightsandkings.knk.paper.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentConfigManagerTest {

    @Test
    void readsCreativeFlagAndCooldownTemplateFromConfig() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("custom-enchantments.disable-for-creative", true);
        configuration.set("custom-enchantments.cooldown-message", "&eWait %seconds%s");

        Plugin plugin = pluginProxy(configuration, new AtomicBoolean(false));
        EnchantmentConfigManager manager = new EnchantmentConfigManager(plugin);

        assertTrue(manager.disableForCreative());
        assertEquals("&eWait %seconds%s", manager.cooldownMessageTemplate());
    }

    @Test
    void resolvesMessagesWithPlaceholderReplacement() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("custom-enchantments.messages.cmd-clear", "&aRemoved cooldowns for %player%.");

        Plugin plugin = pluginProxy(configuration, new AtomicBoolean(false));
        EnchantmentConfigManager manager = new EnchantmentConfigManager(plugin);

        String result = manager.getMessage(
                "messages.cmd-clear",
                "fallback",
                Map.of("%player%", "Pandi")
        );

        assertEquals("&aRemoved cooldowns for Pandi.", result);
    }

    @Test
    void reloadDelegatesToPlugin() {
        AtomicBoolean reloadCalled = new AtomicBoolean(false);
        Plugin plugin = pluginProxy(new YamlConfiguration(), reloadCalled);
        EnchantmentConfigManager manager = new EnchantmentConfigManager(plugin);

        manager.reload();

        assertTrue(reloadCalled.get());
    }

    private Plugin pluginProxy(FileConfiguration configuration, AtomicBoolean reloadCalled) {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("getConfig".equals(method.getName())) {
                        return configuration;
                    }
                    if ("reloadConfig".equals(method.getName())) {
                        reloadCalled.set(true);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}

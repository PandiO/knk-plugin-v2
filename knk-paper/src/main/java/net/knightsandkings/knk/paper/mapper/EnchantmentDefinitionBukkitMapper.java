package net.knightsandkings.knk.paper.mapper;

import net.knightsandkings.knk.core.domain.enchantments.KnkEnchantmentDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

/**
 * Converts persisted EnchantmentDefinition data into Bukkit enchantment resolution data.
 */
public final class EnchantmentDefinitionBukkitMapper {

    private EnchantmentDefinitionBukkitMapper() {
    }

    public static BukkitEnchantmentResolution toBukkit(KnkEnchantmentDefinition definition) {
        if (definition == null) {
            return BukkitEnchantmentResolution.invalid("Enchantment definition is null");
        }

        String namespaceKey = resolveNamespaceKey(definition);
        if (namespaceKey == null || namespaceKey.isBlank()) {
            return BukkitEnchantmentResolution.invalid(
                    "No valid Minecraft enchantment namespace key found (expected baseEnchantmentNamespaceKey or minecraft:* key)"
            );
        }

        NamespacedKey namespacedKey = NamespacedKey.fromString(namespaceKey);
        if (namespacedKey == null) {
            return BukkitEnchantmentResolution.invalid("Invalid namespace key format: " + namespaceKey);
        }

        Enchantment enchantment = Registry.ENCHANTMENT.get(namespacedKey);
        if (enchantment == null) {
            enchantment = Enchantment.getByKey(namespacedKey);
        }

        if (enchantment == null) {
            return BukkitEnchantmentResolution.invalid(
                    "Enchantment is not registered on this server: " + namespaceKey
            );
        }

        int configuredMaxLevel = definition.maxLevel() != null ? definition.maxLevel() : 1;
        int defaultLevel = Math.max(1, Math.min(configuredMaxLevel, enchantment.getMaxLevel()));

        return new BukkitEnchantmentResolution(enchantment, namespaceKey, defaultLevel, null);
    }

    private static String resolveNamespaceKey(KnkEnchantmentDefinition definition) {
        if (definition.baseEnchantmentNamespaceKey() != null && !definition.baseEnchantmentNamespaceKey().isBlank()) {
            return definition.baseEnchantmentNamespaceKey();
        }

        if (definition.key() != null && definition.key().startsWith("minecraft:")) {
            return definition.key();
        }

        return null;
    }

    public record BukkitEnchantmentResolution(
            Enchantment enchantment,
            String namespaceKey,
            int defaultLevel,
            String error
    ) {
        public boolean isValid() {
            return enchantment != null && error == null;
        }

        public static BukkitEnchantmentResolution invalid(String error) {
            return new BukkitEnchantmentResolution(null, null, 0, error);
        }
    }
}

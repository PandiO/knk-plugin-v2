package net.knightsandkings.knk.core.domain.enchantments;

/**
 * Domain model for EnchantmentDefinition.
 *
 * @param id Entity ID
 * @param key Internal key (e.g. minecraft:sharpness, knk:lifesteal)
 * @param displayName Display name
 * @param description Description
 * @param isCustom True if this is a custom enchantment definition
 * @param maxLevel Max allowed level
 * @param minecraftEnchantmentRefId Optional linked minecraft enchantment ref ID
 * @param baseEnchantmentNamespaceKey Optional minecraft namespace key for base enchantment
 */
public record KnkEnchantmentDefinition(
        Integer id,
        String key,
        String displayName,
        String description,
        Boolean isCustom,
        Integer maxLevel,
        Integer minecraftEnchantmentRefId,
        String baseEnchantmentNamespaceKey
) {}

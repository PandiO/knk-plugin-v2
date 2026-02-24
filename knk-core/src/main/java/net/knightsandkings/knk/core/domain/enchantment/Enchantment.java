package net.knightsandkings.knk.core.domain.enchantment;

public record Enchantment(
        String id,
        String displayName,
        int maxLevel,
        int cooldownMs,
        EnchantmentType type,
        Double triggerProbability
) {}

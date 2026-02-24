package net.knightsandkings.knk.core.domain.item;

public record KnkItemBlueprintDefaultEnchantment(
        Integer itemBlueprintId,
        Integer enchantmentDefinitionId,
        Integer level,
        String enchantmentKey,
        String enchantmentDisplayName,
        Integer enchantmentMaxLevel,
        Boolean enchantmentIsCustom
) {}

package net.knightsandkings.knk.core.domain.item;

import java.util.List;

public record KnkItemBlueprint(
        Integer id,
        String name,
        String description,
        Integer iconMaterialRefId,
        String iconNamespaceKey,
        String defaultDisplayName,
        String defaultDisplayDescription,
        Integer defaultQuantity,
        Integer maxStackSize,
        List<KnkItemBlueprintDefaultEnchantment> defaultEnchantments,
        Integer defaultEnchantmentsCount
) {}

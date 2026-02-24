package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemBlueprintDefaultEnchantmentDto(
        @JsonProperty("itemBlueprintId") Integer itemBlueprintId,
        @JsonProperty("enchantmentDefinitionId") Integer enchantmentDefinitionId,
        @JsonProperty("enchantmentDefinition") EnchantmentDefinitionNavDto enchantmentDefinition,
        @JsonProperty("level") Integer level
) {}

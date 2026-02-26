package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnchantmentDefinitionListDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("key") String key,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("description") String description,
        @JsonProperty("maxLevel") Integer maxLevel,
        @JsonProperty("isCustom") Boolean isCustom,
        @JsonProperty("minecraftEnchantmentRefId") Integer minecraftEnchantmentRefId,
        @JsonProperty("baseEnchantmentNamespaceKey") String baseEnchantmentNamespaceKey,
        @JsonProperty("blueprintCount") Integer blueprintCount
) {}

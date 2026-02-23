package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnchantmentDefinitionDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("key") String key,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("description") String description,
        @JsonProperty("isCustom") Boolean isCustom,
        @JsonProperty("maxLevel") Integer maxLevel,
        @JsonProperty("minecraftEnchantmentRefId") Integer minecraftEnchantmentRefId,
        @JsonProperty("baseEnchantmentRef") MinecraftEnchantmentRefNavDto baseEnchantmentRef
) {}

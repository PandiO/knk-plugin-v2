package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnchantmentDefinitionNavDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("key") String key,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("maxLevel") Integer maxLevel,
        @JsonProperty("isCustom") Boolean isCustom
) {}

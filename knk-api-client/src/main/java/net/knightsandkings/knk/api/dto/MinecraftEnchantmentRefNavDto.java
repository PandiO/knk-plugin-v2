package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinecraftEnchantmentRefNavDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("namespaceKey") String namespaceKey,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("maxLevel") Integer maxLevel,
        @JsonProperty("iconUrl") String iconUrl
) {}

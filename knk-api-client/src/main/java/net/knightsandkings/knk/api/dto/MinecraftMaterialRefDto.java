package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinecraftMaterialRefDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("namespaceKey") String namespaceKey,
        @JsonProperty("legacyName") String legacyName,
        @JsonProperty("category") String category,
        @JsonProperty("iconUrl") String iconUrl
) {}

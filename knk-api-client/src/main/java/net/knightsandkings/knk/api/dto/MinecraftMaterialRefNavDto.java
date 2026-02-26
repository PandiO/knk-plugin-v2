package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinecraftMaterialRefNavDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("namespaceKey") String namespaceKey,
        @JsonProperty("legacyName") String legacyName,
        @JsonProperty("iconUrl") String iconUrl
) {}

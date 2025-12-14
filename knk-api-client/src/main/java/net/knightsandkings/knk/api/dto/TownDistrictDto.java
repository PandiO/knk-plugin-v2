package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TownDistrictDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("allowEntry") Boolean allowEntry,
        @JsonProperty("allowExit") Boolean allowExit,
        @JsonProperty("wgRegionId") String wgRegionId
) {
}

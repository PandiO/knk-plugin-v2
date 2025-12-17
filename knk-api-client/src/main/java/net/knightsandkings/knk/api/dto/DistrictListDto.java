package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DistrictListDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("wgRegionId") String wgRegionId,
        @JsonProperty("townId") Integer townId,
        @JsonProperty("townName") String townName
) {
}

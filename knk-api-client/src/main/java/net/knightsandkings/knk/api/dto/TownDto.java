package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record TownDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("createdAt") OffsetDateTime createdAt,
        @JsonProperty("allowEntry") Boolean allowEntry,
        @JsonProperty("allowExit") Boolean allowExit,
        @JsonProperty("wgRegionId") String wgRegionId,
        @JsonProperty("locationId") Integer locationId,
        @JsonProperty("location") LocationDto location,
        @JsonProperty("streetIds") List<Integer> streetIds,
        @JsonProperty("streets") List<TownStreetDto> streets,
        @JsonProperty("districtIds") List<Integer> districtIds,
        @JsonProperty("districts") List<TownDistrictDto> districts
) {
}

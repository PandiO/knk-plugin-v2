package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record DistrictDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("allowEntry") Boolean allowEntry,
        @JsonProperty("allowExit") Boolean allowExit,
        @JsonProperty("wgRegionId") String wgRegionId,
        @JsonProperty("locationId") Integer locationId,
        @JsonProperty("location") LocationDto location,
        @JsonProperty("townId") Integer townId,
        @JsonProperty("streetIds") List<Integer> streetIds,
        @JsonProperty("town") DistrictTownDto town,
        @JsonProperty("streets") List<DistrictStreetDto> streets,
        @JsonProperty("structures") List<DistrictStructureDto> structures
) {
}

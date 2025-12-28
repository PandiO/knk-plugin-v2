package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StructureDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("createdAt") java.time.LocalDateTime createdAt,
        @JsonProperty("allowEntry") Boolean allowEntry,
        @JsonProperty("allowExit") Boolean allowExit,
        @JsonProperty("wgRegionId") String wgRegionId,
        @JsonProperty("locationId") Integer locationId,
        @JsonProperty("streetId") Integer streetId,
        @JsonProperty("districtId") Integer districtId,
        @JsonProperty("houseNumber") Integer houseNumber
) {}

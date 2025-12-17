package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DistrictStructureDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("houseNumber") Integer houseNumber,
        @JsonProperty("streetId") Integer streetId
) {
}

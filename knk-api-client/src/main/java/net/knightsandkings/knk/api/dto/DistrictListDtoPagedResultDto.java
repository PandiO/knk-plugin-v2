package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DistrictListDtoPagedResultDto(
        @JsonProperty("items") List<DistrictListDto> items,
        @JsonProperty("totalCount") Integer totalCount,
        @JsonProperty("pageNumber") Integer pageNumber,
        @JsonProperty("pageSize") Integer pageSize
) {
}

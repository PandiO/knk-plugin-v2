package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TownListDtoPagedResultDto(
        @JsonProperty("items") List<TownListDto> items,
        @JsonProperty("totalCount") int totalCount,
        @JsonProperty("pageNumber") int pageNumber,
        @JsonProperty("pageSize") int pageSize
) {
}

package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record PagedQueryDto(
        @JsonProperty("pageNumber") int pageNumber,
        @JsonProperty("pageSize") int pageSize,
        @JsonProperty("searchTerm") String searchTerm,
        @JsonProperty("sortBy") String sortBy,
        @JsonProperty("sortDescending") boolean sortDescending,
        @JsonProperty("filters") Map<String, String> filters
) {
}

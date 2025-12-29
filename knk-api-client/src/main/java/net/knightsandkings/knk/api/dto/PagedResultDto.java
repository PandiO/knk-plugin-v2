package net.knightsandkings.knk.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PagedResultDto<O extends Object> (
    @JsonProperty("items") List<O> items,
    @JsonProperty("totalCount") Integer totalCount,
    @JsonProperty("pageNumber") Integer pageNumber,
    @JsonProperty("pageSize") Integer pageSize
) {
}

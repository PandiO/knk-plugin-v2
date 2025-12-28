package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StructureListDtoPagedResultDto(
        @JsonProperty("items") List<StructureListDto> items,
        @JsonProperty("totalCount") int totalCount,
        @JsonProperty("pageNumber") int pageNumber,
        @JsonProperty("pageSize") int pageSize
) {}

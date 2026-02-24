package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ItemBlueprintListDtoPagedResultDto(
        @JsonProperty("items") List<ItemBlueprintListDto> items,
        @JsonProperty("pageNumber") Integer pageNumber,
        @JsonProperty("pageSize") Integer pageSize,
        @JsonProperty("totalCount") @JsonAlias("totalItems") Long totalCount
) {}

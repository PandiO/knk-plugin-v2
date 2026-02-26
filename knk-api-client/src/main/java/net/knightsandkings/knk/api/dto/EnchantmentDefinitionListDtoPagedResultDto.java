package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EnchantmentDefinitionListDtoPagedResultDto(
        @JsonProperty("items") List<EnchantmentDefinitionListDto> items,
        @JsonProperty("pageNumber") Integer pageNumber,
        @JsonProperty("pageSize") Integer pageSize,
        @JsonProperty("totalItems") Long totalItems,
        @JsonProperty("totalPages") Integer totalPages
) {}

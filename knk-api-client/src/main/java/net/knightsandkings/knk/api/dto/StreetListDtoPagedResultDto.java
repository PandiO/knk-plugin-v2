package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Paged result wrapper for StreetListDto.
 * Maps to StreetListDtoPagedResultDto from swagger.json.
 */
public class StreetListDtoPagedResultDto {

    @JsonProperty("items")
    private List<StreetListDto> items;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("pageNumber")
    private Integer pageNumber;

    @JsonProperty("pageSize")
    private Integer pageSize;

    public List<StreetListDto> getItems() {
        return items;
    }

    public void setItems(List<StreetListDto> items) {
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

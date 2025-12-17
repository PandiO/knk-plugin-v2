package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for street district information embedded in StreetDto.
 * Maps to StreetDistrictDto from swagger.json.
 */
public class StreetDistrictDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("allowEntry")
    private Boolean allowEntry;

    @JsonProperty("allowExit")
    private Boolean allowExit;

    @JsonProperty("wgRegionId")
    private String wgRegionId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAllowEntry() {
        return allowEntry;
    }

    public void setAllowEntry(Boolean allowEntry) {
        this.allowEntry = allowEntry;
    }

    public Boolean getAllowExit() {
        return allowExit;
    }

    public void setAllowExit(Boolean allowExit) {
        this.allowExit = allowExit;
    }

    public String getWgRegionId() {
        return wgRegionId;
    }

    public void setWgRegionId(String wgRegionId) {
        this.wgRegionId = wgRegionId;
    }
}

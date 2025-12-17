package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for full street details from Web API.
 * Maps to StreetDto from swagger.json.
 */
public class StreetDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("districtIds")
    private List<Integer> districtIds;

    @JsonProperty("districts")
    private List<StreetDistrictDto> districts;

    @JsonProperty("structures")
    private List<StreetStructureDto> structures;

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

    public List<Integer> getDistrictIds() {
        return districtIds;
    }

    public void setDistrictIds(List<Integer> districtIds) {
        this.districtIds = districtIds;
    }

    public List<StreetDistrictDto> getDistricts() {
        return districts;
    }

    public void setDistricts(List<StreetDistrictDto> districts) {
        this.districts = districts;
    }

    public List<StreetStructureDto> getStructures() {
        return structures;
    }

    public void setStructures(List<StreetStructureDto> structures) {
        this.structures = structures;
    }
}

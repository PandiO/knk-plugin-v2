package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for street list items from Web API.
 * Maps to StreetListDto from swagger.json.
 */
public class StreetListDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

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
}

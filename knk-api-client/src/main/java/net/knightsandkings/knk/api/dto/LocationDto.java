package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LocationDto(
    @JsonProperty("id") Integer id,
    @JsonProperty("name") String name,
    @JsonProperty("x") Double x,
    @JsonProperty("y") Double y,
    @JsonProperty("z") Double z,
    @JsonProperty("yaw") Float yaw,
    @JsonProperty("pitch") Float pitch,
    @JsonProperty("world") String world
) {
}

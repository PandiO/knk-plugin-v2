package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto (
    @JsonProperty("id")
    Integer id,
    @JsonProperty("username")
    String username,
    @JsonProperty("uuid")
    java.util.UUID uuid,
    @JsonProperty("email")
    String email,
    @JsonProperty("coins")
    Integer coins,
    @JsonProperty("createdAt")
    java.util.Date createdAt
) {
}

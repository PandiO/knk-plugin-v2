package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserListDto (
    @JsonProperty("id") Integer id,
    @JsonProperty("username") String username,
    @JsonProperty("uuid") java.util.UUID uuid,
    @JsonProperty("email") String email,
    @JsonProperty("cash") Integer coins
) {
}

package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for user creation.
 * Wraps the created user and optional link code.
 */
public record CreateUserResponseDto(
    @JsonProperty("user")
    UserResponseDto user,

    @JsonProperty("linkCode")
    LinkCodeResponseDto linkCode
) {
}

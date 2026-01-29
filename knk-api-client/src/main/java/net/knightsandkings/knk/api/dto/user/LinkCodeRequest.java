package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for generating a link code.
 */
public record LinkCodeRequest(
    @JsonProperty("userId")
    Integer userId
) {
}

package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for link code validation.
 * Returned from /api/Users/validate-link-code endpoint.
 */
public record ValidateLinkCodeResponseDto(
    @JsonProperty("isValid")
    Boolean isValid,
    
    @JsonProperty("userId")
    Integer userId,
    
    @JsonProperty("username")
    String username,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("error")
    String error
) {
}

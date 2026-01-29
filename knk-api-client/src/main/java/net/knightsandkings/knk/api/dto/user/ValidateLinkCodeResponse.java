package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for link code validation.
 * Contains validation result and associated user information if valid.
 */
public record ValidateLinkCodeResponse(
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

package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for link code generation.
 * Contains the code and expiration timestamp.
 */
public record LinkCodeResponse(
    @JsonProperty("code")
    String code,
    
    @JsonProperty("expiresAt")
    String expiresAt,
    
    @JsonProperty("formattedCode")
    String formattedCode
) {
}

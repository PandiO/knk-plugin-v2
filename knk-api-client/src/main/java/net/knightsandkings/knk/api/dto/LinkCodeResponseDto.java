package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for link code generation.
 * Returned from /api/Users/generate-link-code endpoint.
 */
public record LinkCodeResponseDto(
    @JsonProperty("code")
    String code,
    
    @JsonProperty("expiresAt")
    String expiresAt,
    
    @JsonProperty("formattedCode")
    String formattedCode
) {
}

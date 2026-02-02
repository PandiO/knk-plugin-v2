package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for duplicate account checking.
 * Returned from /api/Users/check-duplicate endpoint.
 */
public record DuplicateCheckResponseDto(
    @JsonProperty("hasDuplicate")
    Boolean hasDuplicate,
    
    @JsonProperty("conflictingUser")
    UserResponseDto conflictingUser,
    
    @JsonProperty("primaryUser")
    UserResponseDto primaryUser,
    
    @JsonProperty("message")
    String message
) {
}

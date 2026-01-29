package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for duplicate account check.
 * Used to detect if a player has multiple accounts that need merging.
 */
public record DuplicateCheckResponse(
    @JsonProperty("hasDuplicate")
    Boolean hasDuplicate,
    
    @JsonProperty("conflictingUser")
    UserResponse conflictingUser,
    
    @JsonProperty("primaryUser")
    UserResponse primaryUser,
    
    @JsonProperty("message")
    String message
) {
}

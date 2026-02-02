package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for merging two accounts.
 * Used in Phase 3 for account merge flow.
 */
public record MergeAccountsRequestDto(
    @JsonProperty("primaryUserId")
    Integer primaryUserId,
    
    @JsonProperty("secondaryUserId")
    Integer secondaryUserId
) {
}

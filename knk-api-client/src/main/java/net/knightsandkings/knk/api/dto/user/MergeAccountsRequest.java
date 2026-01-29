package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for merging duplicate user accounts.
 * The primary account is kept, the secondary is soft-deleted.
 */
public record MergeAccountsRequest(
    @JsonProperty("primaryUserId")
    Integer primaryUserId,
    
    @JsonProperty("secondaryUserId")
    Integer secondaryUserId
) {
}

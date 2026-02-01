package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for linking an existing account to plugin.
 * Used in Phase 2 for account linking flow.
 */
public record LinkAccountRequestDto(
    @JsonProperty("linkCode")
    String linkCode,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("password")
    String password,
    
    @JsonProperty("passwordConfirmation")
    String passwordConfirmation
) {
}

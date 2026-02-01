package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for changing user password.
 * Used in Phase 4 for password management.
 */
public record ChangePasswordRequestDto(
    @JsonProperty("currentPassword")
    String currentPassword,
    
    @JsonProperty("newPassword")
    String newPassword,
    
    @JsonProperty("passwordConfirmation")
    String passwordConfirmation
) {
}

package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for changing a user's password.
 */
public record ChangePasswordRequest(
    @JsonProperty("currentPassword")
    String currentPassword,
    
    @JsonProperty("newPassword")
    String newPassword,
    
    @JsonProperty("passwordConfirmation")
    String passwordConfirmation
) {
}

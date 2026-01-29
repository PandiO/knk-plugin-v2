package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for linking an account via link code.
 * Used when a player has a Minecraft-only account and wants to add web app credentials.
 */
public record LinkAccountRequest(
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

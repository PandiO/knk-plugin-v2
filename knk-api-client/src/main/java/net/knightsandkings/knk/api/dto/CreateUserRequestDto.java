package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a new user account.
 * Used in Phase 1 for player account creation via plugin.
 */
public record CreateUserRequestDto(
    @JsonProperty("username")
    String username,
    
    @JsonProperty("uuid")
    String uuid,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("password")
    String password,
    
    @JsonProperty("linkCode")
    String linkCode
) {
    /**
     * Create a minimal user with only username and UUID.
     */
    public static CreateUserRequestDto minimalUser(String uuid, String username) {
        return new CreateUserRequestDto(username, uuid, null, null, null);
    }

    /**
     * Create a user for account creation with email and password.
     */
    public static CreateUserRequestDto withCredentials(String uuid, String username, String email, String password) {
        return new CreateUserRequestDto(username, uuid, email, password, null);
    }

    /**
     * Create a user for account linking with link code.
     */
    public static CreateUserRequestDto withLinkCode(String uuid, String username, String linkCode) {
        return new CreateUserRequestDto(username, uuid, null, null, linkCode);
    }
}

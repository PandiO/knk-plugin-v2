package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a new user account.
 * Supports multiple creation flows:
 * - Web app first: email + password (+ optional username)
 * - Minecraft first: UUID + username only
 * - Link existing: linkCode to connect accounts
 */
public record CreateUserRequest(
    @JsonProperty("username")
    String username,
    
    @JsonProperty("uuid")
    String uuid,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("password")
    String password,
    
    @JsonProperty("passwordConfirmation")
    String passwordConfirmation,
    
    @JsonProperty("linkCode")
    String linkCode
) {
    /**
     * Creates a minimal user request (Minecraft first flow).
     * 
     * @param username Player's Minecraft username
     * @param uuid Player's Minecraft UUID
     * @return CreateUserRequest for minimal account
     */
    public static CreateUserRequest minimalUser(String username, String uuid) {
        return new CreateUserRequest(username, uuid, null, null, null, null);
    }
    
    /**
     * Creates a full user request (Web app first flow).
     * 
     * @param username Desired username
     * @param email User's email
     * @param password User's password
     * @param passwordConfirmation Password confirmation
     * @return CreateUserRequest for full account
     */
    public static CreateUserRequest fullUser(String username, String email, String password, String passwordConfirmation) {
        return new CreateUserRequest(username, null, email, password, passwordConfirmation, null);
    }
}

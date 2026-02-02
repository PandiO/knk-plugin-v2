package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for user account information.
 * Returned from API after user creation, linking, or account merge.
 */
public record UserResponseDto(
    @JsonProperty("id")
    Integer id,
    
    @JsonProperty("username")
    String username,
    
    @JsonProperty("uuid")
    String uuid,
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("coins")
    Integer coins,
    
    @JsonProperty("gems")
    Integer gems,
    
    @JsonProperty("experiencePoints")
    Integer experiencePoints,
    
    @JsonProperty("emailVerified")
    Boolean emailVerified,
    
    @JsonProperty("accountCreatedVia")
    String accountCreatedVia
) {
}

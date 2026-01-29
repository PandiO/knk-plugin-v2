package net.knightsandkings.knk.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for user data.
 * Contains complete user information including account status and balances.
 */
public record UserResponse(
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
    String accountCreatedVia,
    
    @JsonProperty("createdAt")
    java.util.Date createdAt
) {
}

package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserSummaryDto (
    @JsonProperty("id") Integer id,
    @JsonProperty("username") String username,
    @JsonProperty("uuid") java.util.UUID uuid,
    @JsonProperty("cash") int coins
    
) {}

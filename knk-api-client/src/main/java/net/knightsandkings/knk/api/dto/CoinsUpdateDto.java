package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for updating user coins. Server JSON field appears to be "cash".
 * TODO: Confirm field name via swagger; change @JsonProperty if needed.
 */
public record CoinsUpdateDto(
    @JsonProperty("cash") int coins
) {}

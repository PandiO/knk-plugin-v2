package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for health status response from API.
 */
public record HealthStatusDto(
    @JsonProperty("status") String status,
    @JsonProperty("version") String version
) {}

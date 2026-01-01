package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for marking a world task as failed.
 */
public record FailTaskDto(
        @JsonProperty("errorMessage") String errorMessage
) {
}

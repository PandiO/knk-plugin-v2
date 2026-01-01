package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for completing a world task with output data.
 */
public record CompleteTaskDto(
        @JsonProperty("outputJson") String outputJson
) {
}

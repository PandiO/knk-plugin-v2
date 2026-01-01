package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for claiming a world task.
 */
public record ClaimTaskDto(
        @JsonProperty("serverId") String serverId,
        @JsonProperty("minecraftUsername") String minecraftUsername
) {
}

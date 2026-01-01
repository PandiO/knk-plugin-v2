package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO for WorldTask entity.
 * Represents a task that requires world-bound data to be gathered in-game.
 */
public record WorldTaskDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("workflowSessionId") Integer workflowSessionId,
        @JsonProperty("entityType") String entityType,
        @JsonProperty("stepNumber") Integer stepNumber,
        @JsonProperty("fieldName") String fieldName,
        @JsonProperty("status") String status,
        @JsonProperty("linkCode") String linkCode,
        @JsonProperty("claimedByServerId") String claimedByServerId,
        @JsonProperty("claimedByMinecraftUsername") String claimedByMinecraftUsername,
        @JsonProperty("claimedAt") LocalDateTime claimedAt,
        @JsonProperty("completedAt") LocalDateTime completedAt,
        @JsonProperty("inputJson") String inputJson,
        @JsonProperty("outputJson") String outputJson,
        @JsonProperty("errorMessage") String errorMessage,
        @JsonProperty("createdAt") LocalDateTime createdAt
) {
}

package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for gate block snapshot from Web API.
 * Represents a single block in a gate structure with its relative position.
 */
public record GateBlockSnapshotDto(
    @JsonProperty("id") Integer id,
    @JsonProperty("gateStructureId") Integer gateStructureId,
    @JsonProperty("relativeX") Integer relativeX,
    @JsonProperty("relativeY") Integer relativeY,
    @JsonProperty("relativeZ") Integer relativeZ,
    @JsonProperty("minecraftBlockRefId") Integer minecraftBlockRefId,
    @JsonProperty("blockData") String blockData,
    @JsonProperty("sortOrder") Integer sortOrder
) {}

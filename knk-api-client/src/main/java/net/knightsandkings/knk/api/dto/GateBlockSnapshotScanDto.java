package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single scanned block, shaped to match the Web API's GateBlockSnapshotCreateDto contract.
 * Distinct from {@link GateBlockSnapshotDto}, which describes the read-path animation contract.
 */
public record GateBlockSnapshotScanDto(
    @JsonProperty("relativeX") int relativeX,
    @JsonProperty("relativeY") int relativeY,
    @JsonProperty("relativeZ") int relativeZ,
    @JsonProperty("worldX") int worldX,
    @JsonProperty("worldY") int worldY,
    @JsonProperty("worldZ") int worldZ,
    @JsonProperty("materialName") String materialName,
    @JsonProperty("blockDataJson") String blockDataJson,
    @JsonProperty("tileEntityJson") String tileEntityJson,
    @JsonProperty("sortOrder") int sortOrder
) {
}

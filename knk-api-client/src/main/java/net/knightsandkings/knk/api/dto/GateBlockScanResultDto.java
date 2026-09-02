package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OutputJson payload produced once a GateBlockScan WorldTask finishes.
 * status must serialize as one of: "Success", "Warning", "Failed" (matches the API's GateBlockScanStatus enum).
 */
public record GateBlockScanResultDto(
    @JsonProperty("status") String status,
    @JsonProperty("blockCount") int blockCount,
    @JsonProperty("snapshots") List<GateBlockSnapshotScanDto> snapshots,
    @JsonProperty("warnings") List<String> warnings,
    @JsonProperty("errorMessage") String errorMessage
) {
}

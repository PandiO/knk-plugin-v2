package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * InputJson payload for a GateBlockScan WorldTask.
 * Full gate geometry is re-fetched via GateStructuresApi.getById(gateStructureId).
 */
public record GateBlockScanRequestDto(
    @JsonProperty("gateStructureId") Integer gateStructureId
) {
}

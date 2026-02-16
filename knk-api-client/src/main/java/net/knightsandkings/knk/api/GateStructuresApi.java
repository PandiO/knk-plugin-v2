package net.knightsandkings.knk.api;

import net.knightsandkings.knk.api.dto.GateBlockSnapshotDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API interface for gate structure operations.
 * Provides methods to fetch gate structures and their block snapshots from the Web API.
 * 
 * This interface is in knk-api-client (not knk-core) because it returns DTOs.
 * Framework adapters (in knk-paper) implement this and use it to load gates into knk-core.
 */
public interface GateStructuresApi {

    /**
     * Fetch all active gate structures from the Web API.
     * Calls GET /api/GateStructures or similar endpoint.
     *
     * @return CompletableFuture with list of all gate structures
     */
    CompletableFuture<List<GateStructureDto>> getAll();

    /**
     * Get a single gate structure by ID.
     * Calls GET /api/GateStructures/{id}
     *
     * @param id Gate structure ID
     * @return CompletableFuture with gate structure details
     */
    CompletableFuture<GateStructureDto> getById(int id);

    /**
     * Update the state (IsOpened) of a gate structure.
     * Calls PUT /api/GateStructures/{id}/state
     *
     * @param id Gate structure ID
     * @param isOpened New opened state
     * @return CompletableFuture that completes when update is done
     */
    CompletableFuture<Void> updateGateState(int id, boolean isOpened, boolean isDestroyed);

    /**
     * Get all block snapshots for a specific gate.
     * Calls GET /api/GateStructures/{id}/snapshots
     *
     * @param gateId Gate structure ID
     * @return CompletableFuture with list of block snapshots
     */
    CompletableFuture<List<GateBlockSnapshotDto>> getGateSnapshots(int gateId);
}

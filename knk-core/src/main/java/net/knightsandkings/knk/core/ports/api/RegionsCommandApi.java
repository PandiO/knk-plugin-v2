package net.knightsandkings.knk.core.ports.api;

import java.util.concurrent.CompletableFuture;

/**
 * API for managing WorldGuard regions via the Web API.
 * Handles region operations that require Web API coordination.
 */
public interface RegionsCommandApi {
    /**
     * Rename a region from oldRegionId to newRegionId.
     * This is typically called after an entity is successfully created/updated
     * to finalize the temporary region name.
     * 
     * @param oldRegionId The current region ID
     * @param newRegionId The desired new region ID
     * @return CompletableFuture<Boolean> - true if successful, false otherwise
     */
    CompletableFuture<Boolean> renameRegion(String oldRegionId, String newRegionId);
}

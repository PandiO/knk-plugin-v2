package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.HealthStatus;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for health check operations.
 * Implementations must execute I/O asynchronously.
 */
public interface HealthApi {
    /**
     * Get health status from the API backend.
     * 
     * @return CompletableFuture with HealthStatus
     * @throws net.knightsandkings.knk.core.exception.ApiException if API call fails
     */
    CompletableFuture<HealthStatus> getHealth();
}

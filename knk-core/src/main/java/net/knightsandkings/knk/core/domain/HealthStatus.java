package net.knightsandkings.knk.core.domain;

/**
 * Health status response from the API backend.
 */
public record HealthStatus(
    String status,
    String version
) {
    public HealthStatus {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status cannot be null or blank");
        }
    }
    
    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(status);
    }
}

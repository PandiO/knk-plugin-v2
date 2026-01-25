package net.knightsandkings.knk.core.dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.knightsandkings.knk.core.domain.HealthStatus;
import net.knightsandkings.knk.core.ports.api.HealthApi;

/**
 * Unit tests for HealthDataAccess gateway.
 */
public class HealthDataAccessTest {
    
    private HealthDataAccess gateway;
    private HealthApi healthApi;
    private HealthStatus testStatus;
    
    @BeforeEach
    void setUp() {
        healthApi = new StubHealthApi();
        gateway = new HealthDataAccess(Duration.ofSeconds(30), healthApi);
        
        testStatus = new HealthStatus("UP", "1.0.0");
    }
    
    @Test
    void testGetHealth_ApiCall() throws Exception {
        // Act
        FetchResult<HealthStatus> result = gateway.getHealthAsync().get();
        
        // Assert
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testStatus, result.value().orElse(null));
    }
    
    @Test
    void testGetHealth_CacheHit() throws Exception {
        // Arrange: First call populates cache
        gateway.getHealthAsync().get();
        
        // Act: Second call should hit cache
        FetchResult<HealthStatus> result = gateway.getHealthAsync().get();
        
        // Assert
        assertEquals(FetchStatus.HIT, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testStatus, result.value().orElse(null));
    }
    
    @Test
    void testRefresh_UpdatesCache() throws Exception {
        // Arrange: First call populates cache
        gateway.getHealthAsync().get();
        
        // Act: Refresh should bypass cache
        FetchResult<HealthStatus> result = gateway.refreshAsync().get();
        
        // Assert
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testStatus, result.value().orElse(null));
    }
    
    @Test
    void testInvalidate_ClearsCache() throws Exception {
        // Arrange: First call populates cache
        gateway.getHealthAsync().get();
        
        // Act
        gateway.invalidate();
        FetchResult<HealthStatus> result = gateway.getHealthAsync().get();
        
        // Assert: Should fetch from API again
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
    }
    
    /**
     * Stub implementation of HealthApi for testing.
     */
    private class StubHealthApi implements HealthApi {
        @Override
        public CompletableFuture<HealthStatus> getHealth() {
            return CompletableFuture.completedFuture(testStatus);
        }
    }
}

package net.knightsandkings.knk.core.dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.knightsandkings.knk.core.cache.TownCache;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.towns.TownSummary;

/**
 * Unit tests for TownsDataAccess gateway.
 */
public class TownsDataAccessTest {
    
    private TownsDataAccess gateway;
    private TownCache cache;
    private TownsQueryApi townsQueryApi;
    private TownDetail testTown;
    
    @BeforeEach
    void setUp() {
        cache = new TownCache(Duration.ofMinutes(30));
        townsQueryApi = new StubTownsQueryApi();
        gateway = new TownsDataAccess(cache, townsQueryApi);
        
        testTown = new TownDetail(
            1,
            "TestTown",
            "Test town description",
            OffsetDateTime.now(),
            true,
            true,
            "testtown_wg",
            null,
            null,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
    
    @Test
    void testGetById_CacheHit() throws Exception {
        // Arrange: Pre-populate cache
        cache.put(testTown);
        
        // Act
        FetchResult<TownDetail> result = gateway.getByIdAsync(1).get();
        
        // Assert
        assertEquals(FetchStatus.HIT, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testTown, result.value().orElse(null));
        assertEquals(DataSource.CACHE, result.source());
    }
    
    @Test
    void testGetById_CacheMissThenApiFetch() throws Exception {
        // Arrange: StubApi returns testTown
        
        // Act
        FetchResult<TownDetail> result = gateway.getByIdAsync(1).get();
        
        // Assert
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testTown, result.value().orElse(null));
        assertEquals(DataSource.API, result.source());
        
        // Verify cache was populated
        assertTrue(cache.get(1).isPresent());
        assertEquals(testTown, cache.get(1).get());
    }
    
    @Test
    void testGetById_NotFound() throws Exception {
        // Arrange: API returns null for unknown ID
        
        // Act
        FetchResult<TownDetail> result = gateway.getByIdAsync(999).get();
        
        // Assert
        assertEquals(FetchStatus.NOT_FOUND, result.status());
        assertFalse(result.isSuccess());
        assertTrue(result.value().isEmpty());
    }
    
    @Test
    void testRefresh_UpdatesCache() throws Exception {
        // Arrange: Cache old town, API returns updated town
        TownDetail oldTown = new TownDetail(
            1,
            "OldName",
            "Old description",
            OffsetDateTime.now(),
            true,
            true,
            "testtown_wg",
            null,
            null,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
        cache.put(oldTown);
        
        // Act
        FetchResult<TownDetail> result = gateway.refreshAsync(1).get();
        
        // Assert
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testTown, result.value().orElse(null));
        
        // Verify cache was updated
        TownDetail cached = cache.get(1).get();
        assertEquals("TestTown", cached.name());
    }
    
    @Test
    void testInvalidate_RemovesFromCache() throws Exception {
        // Arrange: Cache town
        cache.put(testTown);
        assertTrue(cache.get(1).isPresent());
        
        // Act
        gateway.invalidate(1);
        
        // Assert
        assertFalse(cache.get(1).isPresent());
    }
    
    @Test
    void testInvalidateAll_ClearsCache() throws Exception {
        // Arrange: Cache multiple towns
        cache.put(testTown);
        
        // Act
        gateway.invalidateAll();
        
        // Assert
        assertFalse(cache.get(1).isPresent());
        assertEquals(0, cache.size());
    }
    
    /**
     * Stub implementation of TownsQueryApi for testing.
     */
    private class StubTownsQueryApi implements TownsQueryApi {
        @Override
        public CompletableFuture<Page<TownSummary>> search(PagedQuery query) {
            return CompletableFuture.completedFuture(new Page<>(new ArrayList<>(), 0, 0, 10));
        }
        
        @Override
        public CompletableFuture<TownDetail> getById(int id) {
            if (id == 1) {
                return CompletableFuture.completedFuture(testTown);
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}

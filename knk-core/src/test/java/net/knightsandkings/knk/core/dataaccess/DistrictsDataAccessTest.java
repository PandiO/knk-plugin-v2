package net.knightsandkings.knk.core.dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.knightsandkings.knk.core.cache.DistrictCache;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.districts.DistrictSummary;

/**
 * Unit tests for DistrictsDataAccess gateway.
 */
public class DistrictsDataAccessTest {
    
    private DistrictsDataAccess gateway;
    private DistrictCache cache;
    private DistrictsQueryApi districtsQueryApi;
    private DistrictDetail testDistrict;
    
    @BeforeEach
    void setUp() {
        cache = new DistrictCache(Duration.ofMinutes(30));
        districtsQueryApi = new StubDistrictsQueryApi();
        gateway = new DistrictsDataAccess(cache, districtsQueryApi);
        
        testDistrict = new DistrictDetail(
            1,
            "TestDistrict",
            "Test district description",
            OffsetDateTime.now(),
            true,
            true,
            "testdistrict_wg",
            null,
            null,
            1,
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
    
    @Test
    void testGetById_CacheHit() throws Exception {
        // Arrange: Pre-populate cache
        cache.put(testDistrict);
        
        // Act
        FetchResult<DistrictDetail> result = gateway.getByIdAsync(1).get();
        
        // Assert
        assertEquals(FetchStatus.HIT, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testDistrict, result.value().orElse(null));
        assertEquals(DataSource.CACHE, result.source());
    }
    
    @Test
    void testGetById_CacheMissThenApiFetch() throws Exception {
        // Arrange: StubApi returns testDistrict
        
        // Act
        FetchResult<DistrictDetail> result = gateway.getByIdAsync(1).get();
        
        // Assert
        assertEquals(FetchStatus.MISS_FETCHED, result.status());
        assertTrue(result.isSuccess());
        assertEquals(testDistrict, result.value().orElse(null));
        assertEquals(DataSource.API, result.source());
        
        // Verify cache was populated
        assertTrue(cache.get(1).isPresent());
        assertEquals(testDistrict, cache.get(1).get());
    }
    
    @Test
    void testGetById_NotFound() throws Exception {
        // Arrange: API returns null for unknown ID
        
        // Act
        FetchResult<DistrictDetail> result = gateway.getByIdAsync(999).get();
        
        // Assert
        assertEquals(FetchStatus.NOT_FOUND, result.status());
        assertFalse(result.isSuccess());
        assertTrue(result.value().isEmpty());
    }
    
    @Test
    void testInvalidate_RemovesFromCache() throws Exception {
        // Arrange: Cache district
        cache.put(testDistrict);
        assertTrue(cache.get(1).isPresent());
        
        // Act
        gateway.invalidate(1);
        
        // Assert
        assertFalse(cache.get(1).isPresent());
    }
    
    /**
     * Stub implementation of DistrictsQueryApi for testing.
     */
    private class StubDistrictsQueryApi implements DistrictsQueryApi {
        @Override
        public CompletableFuture<Page<DistrictSummary>> search(PagedQuery query) {
            return CompletableFuture.completedFuture(new Page<>(new ArrayList<>(), 0, 0, 10));
        }
        
        @Override
        public CompletableFuture<DistrictDetail> getById(int id) {
            if (id == 1) {
                return CompletableFuture.completedFuture(testDistrict);
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}

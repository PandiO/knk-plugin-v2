package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.LocationDto;
import net.knightsandkings.knk.api.dto.TownDistrictDto;
import net.knightsandkings.knk.api.dto.TownDto;
import net.knightsandkings.knk.api.dto.TownListDto;
import net.knightsandkings.knk.api.dto.TownListDtoPagedResultDto;
import net.knightsandkings.knk.api.dto.TownStreetDto;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.domain.towns.TownSummary;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TownsMapperTest {
    
    @Test
    void testMapSummary() {
        TownListDto dto = new TownListDto(1, "TestTown", "A test town", "region123");
        
        TownSummary summary = TownsMapper.mapSummary(dto);
        
        assertEquals(1, summary.id());
        assertEquals("TestTown", summary.name());
        assertEquals("A test town", summary.description());
        assertEquals("region123", summary.wgRegionId());
    }
    
    @Test
    void testMapSummaryWithNulls() {
        TownListDto dto = new TownListDto(null, null, null, null);
        
        TownSummary summary = TownsMapper.mapSummary(dto);
        
        assertNull(summary.id());
        assertNull(summary.name());
        assertNull(summary.description());
        assertNull(summary.wgRegionId());
    }
    
    @Test
    void testMapPagedList() {
        TownListDto town1 = new TownListDto(1, "Town1", "First town", "region1");
        TownListDto town2 = new TownListDto(2, "Town2", "Second town", "region2");
        TownListDtoPagedResultDto dto = new TownListDtoPagedResultDto(
            List.of(town1, town2),
            100,
            1,
            2
        );
        
        Page<TownSummary> page = TownsMapper.mapPagedList(dto);
        
        assertEquals(2, page.items().size());
        assertEquals(100, page.totalCount());
        assertEquals(1, page.pageNumber());
        assertEquals(2, page.pageSize());
        
        assertEquals("Town1", page.items().get(0).name());
        assertEquals("Town2", page.items().get(1).name());
    }
    
    @Test
    void testMapPagedListWithNullItems() {
        TownListDtoPagedResultDto dto = new TownListDtoPagedResultDto(null, 0, 1, 10);
        
        Page<TownSummary> page = TownsMapper.mapPagedList(dto);
        
        assertNotNull(page.items());
        assertTrue(page.items().isEmpty());
        assertEquals(0, page.totalCount());
    }
    
    @Test
    void testMapDetail() {
        LocationDto location = new LocationDto(1, "Spawn", 100.5, 64.0, -50.3, 180.0f, 0.0f, "world");
        TownStreetDto street = new TownStreetDto(10, "MainStreet");
        TownDistrictDto district = new TownDistrictDto(20, "CityCenter", "Downtown area", true, true, "region1");
        
        TownDto dto = new TownDto(
            1,
            "MyTown",
            "A nice town",
            OffsetDateTime.parse("2024-12-14T10:00:00Z"),
            true,
            false,
            "wg-region-1",
            5,
            location,
            List.of(10, 20),
            List.of(street),
            List.of(20),
            List.of(district)
        );
        
        TownDetail detail = TownsMapper.mapDetail(dto);
        
        assertEquals(1, detail.id());
        assertEquals("MyTown", detail.name());
        assertEquals("A nice town", detail.description());
        assertEquals(true, detail.allowEntry());
        assertEquals(false, detail.allowExit());
        assertEquals("wg-region-1", detail.wgRegionId());
        assertEquals(5, detail.locationId());
        
        // Check location mapping
        assertNotNull(detail.location());
        assertEquals(1, detail.location().id());
        assertEquals("Spawn", detail.location().name());
        assertEquals(100.5, detail.location().x());
        assertEquals(64.0, detail.location().y());
        assertEquals(-50.3, detail.location().z());
        
        // Check streets
        assertEquals(1, detail.streets().size());
        assertEquals("MainStreet", detail.streets().get(0).name());
        
        // Check districts
        assertEquals(1, detail.districts().size());
        assertEquals("CityCenter", detail.districts().get(0).name());
        assertEquals("Downtown area", detail.districts().get(0).description());
    }
    
    @Test
    void testMapDetailWithNulls() {
        TownDto dto = new TownDto(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        TownDetail detail = TownsMapper.mapDetail(dto);
        
        assertNull(detail.id());
        assertNull(detail.name());
        assertNull(detail.location());
        assertNull(detail.streets());
        assertNull(detail.districts());
    }
}

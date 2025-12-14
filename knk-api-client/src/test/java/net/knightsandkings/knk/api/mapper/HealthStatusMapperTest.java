package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.HealthStatusDto;
import net.knightsandkings.knk.core.domain.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthStatusMapperTest {
    
    @Test
    void shouldMapDtoToDomain() {
        HealthStatusDto dto = new HealthStatusDto("UP", "1.0.0");
        HealthStatus domain = HealthStatusMapper.toDomain(dto);
        
        assertNotNull(domain);
        assertEquals("UP", domain.status());
        assertEquals("1.0.0", domain.version());
    }
    
    @Test
    void shouldMapDomainToDto() {
        HealthStatus domain = new HealthStatus("UP", "1.0.0");
        HealthStatusDto dto = HealthStatusMapper.toDto(domain);
        
        assertNotNull(dto);
        assertEquals("UP", dto.status());
        assertEquals("1.0.0", dto.version());
    }
    
    @Test
    void shouldHandleNullDto() {
        assertNull(HealthStatusMapper.toDomain(null));
    }
    
    @Test
    void shouldHandleNullDomain() {
        assertNull(HealthStatusMapper.toDto(null));
    }
    
    @Test
    void shouldHandleNullVersion() {
        HealthStatusDto dto = new HealthStatusDto("UP", null);
        HealthStatus domain = HealthStatusMapper.toDomain(dto);
        
        assertNotNull(domain);
        assertEquals("UP", domain.status());
        assertNull(domain.version());
    }
}

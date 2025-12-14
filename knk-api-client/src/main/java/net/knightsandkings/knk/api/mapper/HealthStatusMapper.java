package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.HealthStatusDto;
import net.knightsandkings.knk.core.domain.HealthStatus;

/**
 * Mapper between HealthStatus domain and DTO.
 */
public class HealthStatusMapper {
    
    public static HealthStatus toDomain(HealthStatusDto dto) {
        if (dto == null) {
            return null;
        }
        return new HealthStatus(dto.status(), dto.version());
    }
    
    public static HealthStatusDto toDto(HealthStatus domain) {
        if (domain == null) {
            return null;
        }
        return new HealthStatusDto(domain.status(), domain.version());
    }
}

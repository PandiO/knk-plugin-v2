package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.LocationDto;
import net.knightsandkings.knk.core.domain.location.KnkLocation;

public final class LocationMapper {
    private LocationMapper() {}

    public static KnkLocation toCore(LocationDto dto) {
        if (dto == null) return null;
        return new KnkLocation(
                dto.id(),
                dto.name(),
                dto.x(),
                dto.y(),
                dto.z(),
                dto.yaw(),
                dto.pitch(),
                dto.world()
        );
    }

    public static LocationDto toDto(KnkLocation loc) {
        if (loc == null) return null;
        return new LocationDto(
                loc.id(),
                loc.name(),
                loc.x(),
                loc.y(),
                loc.z(),
                loc.yaw(),
                loc.pitch(),
                loc.world()
        );
    }
}

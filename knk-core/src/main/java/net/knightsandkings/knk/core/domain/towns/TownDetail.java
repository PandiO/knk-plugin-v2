package net.knightsandkings.knk.core.domain.towns;

import java.time.OffsetDateTime;
import java.util.List;

public record TownDetail(
        Integer id,
        String name,
        String description,
        OffsetDateTime createdAt,
        Boolean allowEntry,
        Boolean allowExit,
        String wgRegionId,
        Integer locationId,
        Location location,
        List<Integer> streetIds,
        List<TownStreet> streets,
        List<Integer> districtIds,
        List<TownDistrict> districts
) {
    public record Location(Integer id, String name, Double x, Double y, Double z, Float yaw, Float pitch, String world) {}
    public record TownStreet(Integer id, String name) {}
    public record TownDistrict(Integer id, String name, String description, Boolean allowEntry, Boolean allowExit, String wgRegionId) {}
}

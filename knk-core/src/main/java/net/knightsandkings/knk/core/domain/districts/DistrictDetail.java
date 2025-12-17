package net.knightsandkings.knk.core.domain.districts;

import java.time.OffsetDateTime;
import java.util.List;

public record DistrictDetail(
        Integer id,
        String name,
        String description,
        OffsetDateTime createdAt,
        Boolean allowEntry,
        Boolean allowExit,
        String wgRegionId,
        Integer locationId,
        Location location,
        Integer townId,
        List<Integer> streetIds,
        Town town,
        List<Street> streets,
        List<Structure> structures
) {
    public record Location(Integer id, String name, Double x, Double y, Double z, Float yaw, Float pitch, String world) {}
    public record Town(Integer id, String name, String description, Boolean allowEntry, Boolean allowExit, String wgRegionId) {}
    public record Street(Integer id, String name) {}
    public record Structure(Integer id, String name, String description, Integer houseNumber, Integer streetId) {}
}

package net.knightsandkings.knk.core.domain.structures;

/**
 * Lightweight structure projection returned by search endpoints.
 */
public record StructureSummary(
    Integer id,
    String name,
    String description,
    String wgRegionId,
    Integer houseNumber,
    Integer streetId,
    String streetName,
    Integer districtId,
    String districtName
) {}

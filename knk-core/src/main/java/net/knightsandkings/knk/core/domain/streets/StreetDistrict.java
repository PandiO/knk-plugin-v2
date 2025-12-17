package net.knightsandkings.knk.core.domain.streets;

/**
 * District information embedded in a Street detail view.
 * READ-ONLY: wgRegionId displayed but not mutated.
 */
public record StreetDistrict(
    Integer id,
    String name,
    String description,
    Boolean allowEntry,
    Boolean allowExit,
    String wgRegionId
) {}

package net.knightsandkings.knk.core.domain.streets;

/**
 * Structure information embedded in a Street detail view.
 * READ-ONLY: no structure creation/update operations.
 */
public record StreetStructure(
    Integer id,
    String name,
    String description,
    Integer houseNumber,
    Integer districtId
) {}

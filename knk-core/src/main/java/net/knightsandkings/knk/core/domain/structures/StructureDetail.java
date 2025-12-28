package net.knightsandkings.knk.core.domain.structures;

import java.time.OffsetDateTime;

/**
 * Full structure projection from Web API.
 */
public record StructureDetail(
    Integer id,
    String name,
    String description,
    OffsetDateTime createdAt,
    Boolean allowEntry,
    Boolean allowExit,
    String wgRegionId,
    Integer locationId,
    Integer streetId,
    Integer districtId,
    Integer houseNumber
) {}

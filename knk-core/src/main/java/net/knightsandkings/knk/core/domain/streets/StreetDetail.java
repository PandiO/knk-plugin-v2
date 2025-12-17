package net.knightsandkings.knk.core.domain.streets;

import java.util.List;

/**
 * Full street detail from Web API.
 * Corresponds to StreetDto from Web API.
 * READ-ONLY: no create/update operations.
 */
public record StreetDetail(
    Integer id,
    String name,
    List<Integer> districtIds,
    List<StreetDistrict> districts,
    List<StreetStructure> structures
) {}

package net.knightsandkings.knk.core.domain.domains;

import java.util.Set;

public record DomainRegionQuery(
    Set<String> wgRegionIds,
    Boolean topDownHierarchy
) {}

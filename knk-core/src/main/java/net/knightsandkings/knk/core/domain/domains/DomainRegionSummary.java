package net.knightsandkings.knk.core.domain.domains;

import java.util.Collection;

public record DomainRegionSummary (
    Integer id,
    String name,
    String description,
    String wgRegionId,
    Boolean allowEntry,
    Boolean allowExit,
    String domainType,
    Collection<DomainRegionSummary> parentDomainDecisions
) {}

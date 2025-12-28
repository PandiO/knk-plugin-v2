package net.knightsandkings.knk.core.ports.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import net.knightsandkings.knk.core.domain.domains.DomainRegionQuery;
import net.knightsandkings.knk.core.domain.domains.DomainRegionSummary;

public interface DomainsQueryApi {
    CompletableFuture<DomainRegionSummary> getByWorldGuardRegionId(String wgRegionId);

    CompletableFuture<HashMap<Integer, DomainRegionSummary>> searchDomainRegionDecisions(DomainRegionQuery query);
}

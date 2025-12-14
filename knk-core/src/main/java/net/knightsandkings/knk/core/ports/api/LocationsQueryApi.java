package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.location.KnkLocation;

import java.util.concurrent.CompletableFuture;

public interface LocationsQueryApi {
    CompletableFuture<Page<KnkLocation>> search(PagedQuery query);
    CompletableFuture<KnkLocation> getById(int id);
}

package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.domain.towns.TownSummary;

import java.util.concurrent.CompletableFuture;

public interface TownsQueryApi {
    CompletableFuture<Page<TownSummary>> search(PagedQuery query);
    CompletableFuture<TownDetail> getById(int id);
}

package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.streets.StreetDetail;
import net.knightsandkings.knk.core.domain.streets.StreetSummary;

import java.util.concurrent.CompletableFuture;

/**
 * Read-only query interface for Streets from Web API.
 * NO create/update/delete operations (per READ-ONLY migration mode).
 */
public interface StreetsQueryApi {

    /**
     * Search streets with pagination.
     * Calls POST /api/Streets/search
     *
     * @param query paged query with search term, filters, sort
     * @return CompletableFuture with paged results
     */
    CompletableFuture<Page<StreetSummary>> search(PagedQuery query);

    /**
     * Get a single street by ID.
     * Calls GET /api/Streets/{id}
     *
     * @param id Street ID
     * @return CompletableFuture with StreetDetail
     */
    CompletableFuture<StreetDetail> getById(int id);
}

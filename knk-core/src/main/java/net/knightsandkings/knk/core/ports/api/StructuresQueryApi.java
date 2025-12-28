package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.structures.StructureDetail;
import net.knightsandkings.knk.core.domain.structures.StructureSummary;

import java.util.concurrent.CompletableFuture;

/**
 * Read-only query interface for Structures from Web API.
 * Uses /api/Structures/search and /api/Structures/{id}.
 */
public interface StructuresQueryApi {
    CompletableFuture<Page<StructureSummary>> search(PagedQuery query);
    CompletableFuture<StructureDetail> getById(int id);
}

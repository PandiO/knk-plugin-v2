package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.item.KnkItemBlueprint;

import java.util.concurrent.CompletableFuture;

public interface ItemBlueprintsQueryApi {
    CompletableFuture<Page<KnkItemBlueprint>> search(PagedQuery query);
    CompletableFuture<KnkItemBlueprint> getById(int id);
}

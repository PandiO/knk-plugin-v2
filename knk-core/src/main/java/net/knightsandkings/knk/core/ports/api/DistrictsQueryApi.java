package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail;
import net.knightsandkings.knk.core.domain.districts.DistrictSummary;

import java.util.concurrent.CompletableFuture;

public interface DistrictsQueryApi {
    CompletableFuture<Page<DistrictSummary>> search(PagedQuery query);
    CompletableFuture<DistrictDetail> getById(int id);
}

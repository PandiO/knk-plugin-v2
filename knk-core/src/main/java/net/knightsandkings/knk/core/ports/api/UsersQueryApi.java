package net.knightsandkings.knk.core.ports.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.users.UserDetail;
import net.knightsandkings.knk.core.domain.users.UserListItem;
import net.knightsandkings.knk.core.domain.users.UserSummary;

public interface UsersQueryApi {
    CompletableFuture<UserDetail> getById(int id);
    CompletableFuture<UserSummary> getByUuid(UUID uuid);
    CompletableFuture<UserSummary> getByUsername(String username);
    CompletableFuture<Page<UserListItem>> search(PagedQuery query);
}
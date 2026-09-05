package net.knightsandkings.knk.core.ports.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.knightsandkings.knk.core.domain.users.GatePassThroughMethod;
import net.knightsandkings.knk.core.domain.users.UserDetail;

/**
 * Command-side operations for users (CQRS).
 * TODO: Enable only when migration mode allows writes.
 */
public interface UsersCommandApi {
    CompletableFuture<Void> setCoinsById(int id, int coins);
    CompletableFuture<Void> setCoinsByUuid(UUID uuid, int coins);
    CompletableFuture<UserDetail> create(UserDetail user);

    /**
     * Persist a player's preferred gate pass-through method (set via /knk gate passthrough).
     */
    CompletableFuture<Void> setGatePassThroughMethodById(int id, GatePassThroughMethod method);
}

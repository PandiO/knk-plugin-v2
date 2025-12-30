package net.knightsandkings.knk.core.cache;

import java.time.Duration;
import java.util.UUID;
import net.knightsandkings.knk.core.domain.users.UserSummary;

/**
 * Type-safe cache for User entities keyed by Minecraft UUID.
 * <p>
 * Unlike region-based caches, this cache uses UUID as the primary key
 * to support player-centric lookups without secondary indices.
 */
public class UserCache extends BaseCache<UUID, UserSummary> {

    public UserCache(Duration ttl) {
        super(ttl);
    }

    /**
     * Retrieves a user by their Minecraft UUID.
     *
     * @param uuid The player's UUID
     * @return Optional containing the user if cached and not expired
     */
    public java.util.Optional<UserSummary> getByUuid(UUID uuid) {
        return get(uuid);
    }

    /**
     * Stores a user in the cache indexed by UUID.
     *
     * @param user The user summary to cache
     */
    public void put(UserSummary user) {
        if (user == null || user.uuid() == null) {
            return;
        }
        put(user.uuid(), user);
    }
}

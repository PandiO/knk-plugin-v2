package net.knightsandkings.knk.core.domain.users;

import java.util.Optional;
import java.util.UUID;

public record UserSummary(
    Integer id,
    String username,
    UUID uuid,
    int coins,
    boolean isNewUser
) {
    // Constructor without isNewUser - defaults to false
    public UserSummary(Integer id, String username, UUID uuid, int coins) {
        this(id, username, uuid, coins, false);
    }
}

package net.knightsandkings.knk.core.domain.users;

import java.util.Optional;
import java.util.UUID;

public record UserSummary(
    Integer id,
    String username,
    UUID uuid,
    String email,
    int coins,
    int gems,
    int experiencePoints,
    boolean isFullAccount,
    boolean isNewUser
) {
    // Constructor without isNewUser - defaults to false
    public UserSummary(Integer id, String username, UUID uuid, String email, int coins, int gems, int experiencePoints, boolean isFullAccount) {
        this(id, username, uuid, email, coins, gems, experiencePoints, isFullAccount, false);
    }
    
    // Legacy constructor for backwards compatibility (minimal user data)
    public UserSummary(Integer id, String username, UUID uuid, int coins) {
        this(id, username, uuid, null, coins, 0, 0, false, false);
    }
}

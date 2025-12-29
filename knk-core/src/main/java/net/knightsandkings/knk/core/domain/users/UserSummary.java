package net.knightsandkings.knk.core.domain.users;

import java.util.UUID;

public record UserSummary(
    Integer id,
    String username,
    UUID uuid,
    int coins
) {
}

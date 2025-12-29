package net.knightsandkings.knk.core.domain.users;

import java.util.Date;

public record UserDetail (
    Integer id,
    String username,
    java.util.UUID uuid,
    String email,
    Integer coins,
    Date createdAt
) {
}

package net.knightsandkings.knk.core.domain.users;

public record UserListItem(
    Integer id,
    String username,
    java.util.UUID uuid,
    String email,
    Integer coins
) {
}
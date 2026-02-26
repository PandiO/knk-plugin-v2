package net.knightsandkings.knk.core.domain.enchantment;

import java.util.UUID;

public record CooldownState(
        UUID playerId,
        String enchantmentId,
        long expiryTimeMs
) {}

package net.knightsandkings.knk.core.ports.enchantment;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CooldownManager {
    CompletableFuture<Long> getRemainingCooldown(UUID playerId, String enchantmentId);
    CompletableFuture<Void> applyCooldown(UUID playerId, String enchantmentId, long durationMs);
    CompletableFuture<Void> clearCooldowns(UUID playerId);
}

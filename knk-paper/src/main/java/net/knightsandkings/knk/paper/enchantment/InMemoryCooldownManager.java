package net.knightsandkings.knk.paper.enchantment;

import net.knightsandkings.knk.core.ports.enchantment.CooldownManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCooldownManager implements CooldownManager {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Long> getRemainingCooldown(UUID playerId, String enchantmentId) {
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return CompletableFuture.completedFuture(0L);
        }

        Long expiry = playerCooldowns.get(enchantmentId);
        if (expiry == null) {
            return CompletableFuture.completedFuture(0L);
        }

        if (expiry <= now) {
            playerCooldowns.remove(enchantmentId);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
            return CompletableFuture.completedFuture(0L);
        }

        return CompletableFuture.completedFuture(expiry - now);
    }

    @Override
    public CompletableFuture<Void> applyCooldown(UUID playerId, String enchantmentId, long durationMs) {
        long expiry = System.currentTimeMillis() + Math.max(durationMs, 0L);
        cooldowns.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(enchantmentId, expiry);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
        return CompletableFuture.completedFuture(null);
    }
}

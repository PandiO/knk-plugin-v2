package net.knightsandkings.knk.core.ports.enchantment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EnchantmentExecutor {
    CompletableFuture<Void> executeOnMeleeHit(
            Map<String, Integer> enchantments,
            UUID attackerId,
            UUID targetId,
            double damageDealt
    );

    CompletableFuture<Boolean> executeOnInteract(Map<String, Integer> enchantments, UUID playerId);

    CompletableFuture<Void> executeOnBowShoot(
            Map<String, Integer> enchantments,
            UUID shooterId,
            UUID projectileId
    );
}

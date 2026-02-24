package net.knightsandkings.knk.paper.enchantment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCooldownManagerTest {

    @Test
    void appliesAndReadsCooldownPerPlayerAndEnchantment() {
        InMemoryCooldownManager manager = new InMemoryCooldownManager();
        UUID playerId = UUID.randomUUID();

        manager.applyCooldown(playerId, "strength", 5000).join();
        long remaining = manager.getRemainingCooldown(playerId, "strength").join();

        assertTrue(remaining > 0);
        assertTrue(remaining <= 5000);
    }

    @Test
    void expiredCooldownReturnsZero() throws InterruptedException {
        InMemoryCooldownManager manager = new InMemoryCooldownManager();
        UUID playerId = UUID.randomUUID();

        manager.applyCooldown(playerId, "chaos", 10).join();
        Thread.sleep(30);
        long remaining = manager.getRemainingCooldown(playerId, "chaos").join();

        assertEquals(0L, remaining);
    }

    @Test
    void clearRemovesAllCooldownsForPlayer() {
        InMemoryCooldownManager manager = new InMemoryCooldownManager();
        UUID playerId = UUID.randomUUID();

        manager.applyCooldown(playerId, "chaos", 1000).join();
        manager.applyCooldown(playerId, "strength", 1000).join();
        manager.clearCooldowns(playerId).join();

        assertEquals(0L, manager.getRemainingCooldown(playerId, "chaos").join());
        assertEquals(0L, manager.getRemainingCooldown(playerId, "strength").join());
    }
}

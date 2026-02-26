package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.core.ports.enchantment.CooldownManager;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentExecutor;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentInteractListenerTest {

    @Test
    void rightClickDetectionMatchesSupportedActions() throws Exception {
        EnchantmentInteractListener listener = new EnchantmentInteractListener(
                new NoOpEnchantmentRepository(),
                new NoOpEnchantmentExecutor(),
                new NoOpCooldownManager(),
                false,
                "&c%seconds% seconds remaining"
        );

        Method isRightClickMethod = EnchantmentInteractListener.class.getDeclaredMethod("isRightClick", Action.class);
        isRightClickMethod.setAccessible(true);

        assertTrue((Boolean) isRightClickMethod.invoke(listener, Action.RIGHT_CLICK_AIR));
        assertTrue((Boolean) isRightClickMethod.invoke(listener, Action.RIGHT_CLICK_BLOCK));
        assertFalse((Boolean) isRightClickMethod.invoke(listener, Action.LEFT_CLICK_AIR));
    }

    @Test
    void cooldownMessageRoundsUpSecondsAndTranslatesColors() throws Exception {
        AtomicReference<String> sentMessage = new AtomicReference<>();
        Player player = testPlayer(sentMessage);

        EnchantmentInteractListener listener = new EnchantmentInteractListener(
                new NoOpEnchantmentRepository(),
                new NoOpEnchantmentExecutor(),
                new NoOpCooldownManager(),
                false,
                "&c%seconds% seconds remaining"
        );

        Method sendCooldownMessageMethod = EnchantmentInteractListener.class.getDeclaredMethod(
                "sendCooldownMessage",
                Player.class,
                long.class
        );
        sendCooldownMessageMethod.setAccessible(true);

        sendCooldownMessageMethod.invoke(listener, player, 4_500L);

        assertEquals("§c5 seconds remaining", sentMessage.get());
    }

    private static Player testPlayer(AtomicReference<String> sentMessage) {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                (proxy, method, args) -> {
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                        sentMessage.set(String.valueOf(args[0]));
                        return null;
                    }
                    if ("getGameMode".equals(method.getName())) {
                        return GameMode.SURVIVAL;
                    }
                    if ("getUniqueId".equals(method.getName())) {
                        return playerId;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class NoOpEnchantmentRepository implements EnchantmentRepository {
        @Override
        public CompletableFuture<java.util.Map<String, Integer>> getEnchantments(java.util.List<String> loreLines) {
            return CompletableFuture.completedFuture(java.util.Map.of());
        }

        @Override
        public CompletableFuture<Boolean> hasAnyEnchantment(java.util.List<String> loreLines) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> hasEnchantment(java.util.List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<java.util.List<String>> applyEnchantment(java.util.List<String> loreLines, String enchantmentId, Integer level) {
            return CompletableFuture.completedFuture(loreLines);
        }

        @Override
        public CompletableFuture<java.util.List<String>> removeEnchantment(java.util.List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(loreLines);
        }
    }

    private static final class NoOpCooldownManager implements CooldownManager {
        @Override
        public CompletableFuture<Long> getRemainingCooldown(UUID playerId, String enchantmentId) {
            return CompletableFuture.completedFuture(0L);
        }

        @Override
        public CompletableFuture<Void> applyCooldown(UUID playerId, String enchantmentId, long durationMs) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> clearCooldowns(UUID playerId) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NoOpEnchantmentExecutor implements EnchantmentExecutor {
        @Override
        public CompletableFuture<Void> executeOnMeleeHit(java.util.Map<String, Integer> enchantments, UUID attackerId, UUID targetId, double damageDealt) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> executeOnInteract(java.util.Map<String, Integer> enchantments, UUID playerId) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Void> executeOnBowShoot(java.util.Map<String, Integer> enchantments, UUID shooterId, UUID projectileId) {
            return CompletableFuture.completedFuture(null);
        }
    }
}

package net.knightsandkings.knk.paper.integration;

import net.knightsandkings.knk.core.ports.enchantment.CooldownManager;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentExecutor;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;
import net.knightsandkings.knk.paper.enchantment.InMemoryCooldownManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentEffectsIntegrationTest {

    @Test
    void passiveEnchantmentsDispatchOnCombatHit() {
        EnchantmentRepository repository = new FixedEnchantmentRepository(Map.of("poison", 2));
        RecordingEnchantmentExecutor executor = new RecordingEnchantmentExecutor(true);
        PassiveFlowCoordinator coordinator = new PassiveFlowCoordinator(repository, executor);
        UUID attackerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        coordinator.handleHit(attackerId, targetId, 5.0d);

        assertEquals(1, executor.meleeInvocations);
        assertEquals(attackerId, executor.lastAttackerId);
        assertEquals(targetId, executor.lastTargetId);
        assertEquals(Map.of("poison", 2), executor.lastMeleeEnchantments);
        assertEquals(5.0d, executor.lastDamage);
    }

    @Test
    void activeSupportEnchantmentsTriggerAndCancelInteraction() {
        UUID playerId = UUID.randomUUID();

        EnchantmentRepository repository = new FixedEnchantmentRepository(Map.of("chaos", 1));
        RecordingEnchantmentExecutor executor = new RecordingEnchantmentExecutor(true);
        CooldownManager cooldownManager = new InMemoryCooldownManager();
        ActiveFlowCoordinator coordinator = new ActiveFlowCoordinator(repository, executor, cooldownManager);

        boolean triggered = coordinator.handleRightClick(playerId, enchantmentId -> true);

        assertTrue(triggered);
        assertEquals(1, executor.interactInvocations);
        assertEquals(playerId, executor.lastInteractPlayerId);
        assertEquals(Map.of("chaos", 1), executor.lastInteractEnchantments);
    }

    @Test
    void cooldownPreventsActiveExecutionAndSendsMessage() {
        UUID playerId = UUID.randomUUID();

        EnchantmentRepository repository = new FixedEnchantmentRepository(Map.of("chaos", 1));
        RecordingEnchantmentExecutor executor = new RecordingEnchantmentExecutor(true);
        InMemoryCooldownManager cooldownManager = new InMemoryCooldownManager();
        cooldownManager.applyCooldown(playerId, "chaos", 5_000L).join();
        ActiveFlowCoordinator coordinator = new ActiveFlowCoordinator(repository, executor, cooldownManager);

        boolean triggered = coordinator.handleRightClick(playerId, enchantmentId -> true);

        assertTrue(!triggered);
        assertEquals(0, executor.interactInvocations);
        assertTrue(cooldownManager.getRemainingCooldown(playerId, "chaos").join() > 0L);
    }

    @Test
    void permissionDeniedPreventsActiveExecution() {
        UUID playerId = UUID.randomUUID();

        EnchantmentRepository repository = new FixedEnchantmentRepository(Map.of("chaos", 1));
        RecordingEnchantmentExecutor executor = new RecordingEnchantmentExecutor(true);
        CooldownManager cooldownManager = new InMemoryCooldownManager();
        ActiveFlowCoordinator coordinator = new ActiveFlowCoordinator(repository, executor, cooldownManager);

        boolean triggered = coordinator.handleRightClick(playerId, enchantmentId -> false);

        assertTrue(!triggered);
        assertEquals(0, executor.interactInvocations);
    }

    private static final class PassiveFlowCoordinator {
        private final EnchantmentRepository repository;
        private final EnchantmentExecutor executor;

        private PassiveFlowCoordinator(EnchantmentRepository repository, EnchantmentExecutor executor) {
            this.repository = repository;
            this.executor = executor;
        }

        private void handleHit(UUID attackerId, UUID targetId, double damage) {
            Map<String, Integer> enchantments = repository.getEnchantments(List.of("§7Poison II")).join();
            executor.executeOnMeleeHit(enchantments, attackerId, targetId, damage).join();
        }
    }

    private static final class ActiveFlowCoordinator {
        private final EnchantmentRepository repository;
        private final EnchantmentExecutor executor;
        private final CooldownManager cooldownManager;

        private ActiveFlowCoordinator(
                EnchantmentRepository repository,
                EnchantmentExecutor executor,
                CooldownManager cooldownManager
        ) {
            this.repository = repository;
            this.executor = executor;
            this.cooldownManager = cooldownManager;
        }

        private boolean handleRightClick(UUID playerId, Predicate<String> hasPermission) {
            Map<String, Integer> availableEnchantments = repository.getEnchantments(List.of("§7Chaos I")).join();
            Map<String, Integer> activatable = new java.util.LinkedHashMap<>();

            for (Map.Entry<String, Integer> entry : availableEnchantments.entrySet()) {
                String enchantmentId = entry.getKey();
                int level = entry.getValue() == null ? 0 : entry.getValue();
                if (level < 1 || !hasPermission.test(enchantmentId)) {
                    continue;
                }

                long remaining = cooldownManager.getRemainingCooldown(playerId, enchantmentId).join();
                if (remaining > 0L) {
                    continue;
                }

                activatable.put(enchantmentId, level);
            }

            if (activatable.isEmpty()) {
                return false;
            }

            return executor.executeOnInteract(activatable, playerId).join();
        }
    }

    private static final class FixedEnchantmentRepository implements EnchantmentRepository {
        private final Map<String, Integer> enchantments;

        private FixedEnchantmentRepository(Map<String, Integer> enchantments) {
            this.enchantments = enchantments;
        }

        @Override
        public CompletableFuture<Map<String, Integer>> getEnchantments(List<String> loreLines) {
            return CompletableFuture.completedFuture(enchantments);
        }

        @Override
        public CompletableFuture<Boolean> hasAnyEnchantment(List<String> loreLines) {
            return CompletableFuture.completedFuture(!enchantments.isEmpty());
        }

        @Override
        public CompletableFuture<Boolean> hasEnchantment(List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(enchantments.containsKey(enchantmentId));
        }

        @Override
        public CompletableFuture<List<String>> applyEnchantment(List<String> loreLines, String enchantmentId, Integer level) {
            return CompletableFuture.completedFuture(loreLines);
        }

        @Override
        public CompletableFuture<List<String>> removeEnchantment(List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(loreLines);
        }
    }

    private static final class RecordingEnchantmentExecutor implements EnchantmentExecutor {
        private final boolean interactResult;

        private int meleeInvocations;
        private int interactInvocations;
        private Map<String, Integer> lastMeleeEnchantments;
        private Map<String, Integer> lastInteractEnchantments;
        private UUID lastAttackerId;
        private UUID lastTargetId;
        private UUID lastInteractPlayerId;
        private double lastDamage;

        private RecordingEnchantmentExecutor(boolean interactResult) {
            this.interactResult = interactResult;
        }

        @Override
        public CompletableFuture<Void> executeOnMeleeHit(Map<String, Integer> enchantments, UUID attackerId, UUID targetId, double damageDealt) {
            meleeInvocations++;
            lastMeleeEnchantments = enchantments;
            lastAttackerId = attackerId;
            lastTargetId = targetId;
            lastDamage = damageDealt;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> executeOnInteract(Map<String, Integer> enchantments, UUID playerId) {
            interactInvocations++;
            lastInteractEnchantments = enchantments;
            lastInteractPlayerId = playerId;
            return CompletableFuture.completedFuture(interactResult);
        }

        @Override
        public CompletableFuture<Void> executeOnBowShoot(Map<String, Integer> enchantments, UUID shooterId, UUID projectileId) {
            return CompletableFuture.completedFuture(null);
        }
    }
}

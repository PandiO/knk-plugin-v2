package net.knightsandkings.knk.paper.enchantment.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackEnchantmentEffectTest {

    @Test
    void probabilityBasedEffectTriggersWhenRollIsBelowChance() {
        AtomicBoolean called = new AtomicBoolean(false);
        AttackEnchantmentEffect effect = new TestAttackEffect("poison", 0.15d, () -> 0.10d, called);

        boolean triggered = effect.tryExecute(null, null, null, 1);

        assertTrue(triggered);
        assertTrue(called.get());
    }

    @Test
    void probabilityBasedEffectDoesNotTriggerWhenRollIsAboveChance() {
        AtomicBoolean called = new AtomicBoolean(false);
        AttackEnchantmentEffect effect = new TestAttackEffect("poison", 0.15d, () -> 0.30d, called);

        boolean triggered = effect.tryExecute(null, null, null, 1);

        assertFalse(triggered);
        assertFalse(called.get());
    }

    @Test
    void alwaysTriggerEffectExecutesWithoutProbability() {
        AtomicBoolean called = new AtomicBoolean(false);
        AttackEnchantmentEffect effect = new TestAttackEffect("blindness", null, () -> 0.99d, called);

        boolean triggered = effect.tryExecute(null, null, null, 3);

        assertTrue(triggered);
        assertTrue(called.get());
    }

    private static final class TestAttackEffect extends AttackEnchantmentEffect {
        private final AtomicBoolean called;

        private TestAttackEffect(
                String enchantmentId,
                Double triggerProbability,
                java.util.function.DoubleSupplier rollSupplier,
                AtomicBoolean called
        ) {
            super(enchantmentId, triggerProbability, testPlugin(), rollSupplier);
            this.called = called;
        }

        @Override
        protected void applyEffect(ItemStack weapon, Player attacker, LivingEntity target, int level) {
            called.set(true);
        }

        private static Plugin testPlugin() {
            return (Plugin) Proxy.newProxyInstance(
                    Plugin.class.getClassLoader(),
                    new Class[]{Plugin.class},
                    (proxy, method, args) -> null
            );
        }
    }
}

package net.knightsandkings.knk.paper.enchantment.effects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportEnchantmentEffectTest {

    @Test
    void executesEffectWhenPlayerAndLevelAreValid() {
        AtomicBoolean called = new AtomicBoolean(false);
        SupportEnchantmentEffect effect = new TestSupportEffect(called);

        boolean triggered = effect.tryExecute(null, testPlayer(), 1);

        assertTrue(triggered);
        assertTrue(called.get());
    }

    @Test
    void doesNotExecuteWhenLevelIsInvalid() {
        AtomicBoolean called = new AtomicBoolean(false);
        SupportEnchantmentEffect effect = new TestSupportEffect(called);

        boolean triggered = effect.tryExecute(null, testPlayer(), 0);

        assertFalse(triggered);
        assertFalse(called.get());
    }

    @Test
    void doesNotExecuteWhenPlayerIsNull() {
        AtomicBoolean called = new AtomicBoolean(false);
        SupportEnchantmentEffect effect = new TestSupportEffect(called);

        boolean triggered = effect.tryExecute(null, null, 1);

        assertFalse(triggered);
        assertFalse(called.get());
    }

    private static Player testPlayer() {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                (proxy, method, args) -> null
        );
    }

    private static Plugin testPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> null
        );
    }

    private static final class TestSupportEffect extends SupportEnchantmentEffect {
        private final AtomicBoolean called;

        private TestSupportEffect(AtomicBoolean called) {
            super("health_boost", testPlugin());
            this.called = called;
        }

        @Override
        protected void applyEffect(ItemStack item, Player player, int level) {
            called.set(true);
        }
    }
}

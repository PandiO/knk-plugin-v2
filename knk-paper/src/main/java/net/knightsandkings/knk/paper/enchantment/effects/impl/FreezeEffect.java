package net.knightsandkings.knk.paper.enchantment.effects.impl;

import net.knightsandkings.knk.paper.enchantment.FrozenPlayerTracker;
import net.knightsandkings.knk.paper.enchantment.effects.AttackEnchantmentEffect;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class FreezeEffect extends AttackEnchantmentEffect {
    private final FrozenPlayerTracker frozenPlayerTracker;

    public FreezeEffect(Plugin plugin, FrozenPlayerTracker frozenPlayerTracker) {
        super("freeze", 0.15d, plugin);
        this.frozenPlayerTracker = frozenPlayerTracker;
    }

    @Override
    protected void applyEffect(ItemStack weapon, Player attacker, LivingEntity target, int level) {
        if (!(target instanceof Player frozenTarget)) {
            return;
        }

        int durationTicks = level * 60;
        frozenPlayerTracker.freeze(frozenTarget, durationTicks);

        for (int tick = 0; tick <= durationTicks; tick += 20) {
            Bukkit.getScheduler().runTaskLater(plugin(), () -> {
                if (frozenPlayerTracker.isFrozen(frozenTarget.getUniqueId())) {
                    playEffect(frozenTarget.getLocation().clone().add(0.0d, 0.5d, 0.0d), Effect.STEP_SOUND, 79);
                }
            }, tick);
        }
    }
}

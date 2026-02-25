package net.knightsandkings.knk.paper.enchantment.effects;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public abstract class AttackEnchantmentEffect implements EnchantmentEffect {
    private final String enchantmentId;
    private final Double triggerProbabilityPerLevel;
    private final Plugin plugin;
    private final DoubleSupplier rollSupplier;

    protected AttackEnchantmentEffect(String enchantmentId, Double triggerProbabilityPerLevel, Plugin plugin) {
        this(enchantmentId, triggerProbabilityPerLevel, plugin, () -> ThreadLocalRandom.current().nextDouble());
    }

    protected AttackEnchantmentEffect(
            String enchantmentId,
            Double triggerProbabilityPerLevel,
            Plugin plugin,
            DoubleSupplier rollSupplier
    ) {
        this.enchantmentId = Objects.requireNonNull(enchantmentId, "enchantmentId must not be null");
        this.triggerProbabilityPerLevel = triggerProbabilityPerLevel;
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        this.rollSupplier = Objects.requireNonNull(rollSupplier, "rollSupplier must not be null");
    }

    @Override
    public final String enchantmentId() {
        return enchantmentId;
    }

    @Override
    public final boolean tryExecute(ItemStack weapon, Player attacker, LivingEntity target, int level) {
        if (!shouldTrigger(level)) {
            return false;
        }

        runOnMainThread(() -> applyEffect(weapon, attacker, target, level));
        return true;
    }

    protected abstract void applyEffect(ItemStack weapon, Player attacker, LivingEntity target, int level);

    protected final boolean shouldTrigger(int level) {
        if (level < 1) {
            return false;
        }

        if (triggerProbabilityPerLevel == null) {
            return true;
        }

        double chance = Math.min(1.0d, Math.max(0.0d, triggerProbabilityPerLevel * level));
        return rollSupplier.getAsDouble() < chance;
    }

    protected final void runOnMainThread(Runnable runnable) {
        if (Bukkit.getServer() == null) {
            runnable.run();
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    protected final void playEffect(Location location, Effect effect, int data) {
        if (location == null || location.getWorld() == null || effect == null) {
            return;
        }
        location.getWorld().playEffect(location, effect, data);
    }

    protected final void playSound(Location location, Sound sound, float volume, float pitch) {
        if (location == null || location.getWorld() == null || sound == null) {
            return;
        }
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    protected final Plugin plugin() {
        return plugin;
    }
}

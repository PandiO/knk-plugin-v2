package net.knightsandkings.knk.paper.enchantment.effects.impl;

import net.knightsandkings.knk.paper.enchantment.effects.AttackEnchantmentEffect;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConfusionEffect extends AttackEnchantmentEffect {
    public ConfusionEffect(Plugin plugin) {
        super("confusion", 0.15d, plugin);
    }

    @Override
    protected void applyEffect(ItemStack weapon, Player attacker, LivingEntity target, int level) {
        int durationTicks = level * 60;
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 2));
        playEffect(target.getLocation(), Effect.POTION_BREAK, 0);
    }
}

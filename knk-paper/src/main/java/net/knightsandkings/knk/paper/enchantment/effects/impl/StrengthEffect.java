package net.knightsandkings.knk.paper.enchantment.effects.impl;

import net.knightsandkings.knk.paper.enchantment.effects.AttackEnchantmentEffect;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StrengthEffect extends AttackEnchantmentEffect {
    public StrengthEffect(Plugin plugin) {
        super("strength", 0.15d, plugin);
    }

    @Override
    protected void applyEffect(ItemStack weapon, Player attacker, LivingEntity target, int level) {
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, Math.max(level - 1, 0)));
        playEffect(attacker.getLocation(), Effect.POTION_BREAK, 0);
    }
}

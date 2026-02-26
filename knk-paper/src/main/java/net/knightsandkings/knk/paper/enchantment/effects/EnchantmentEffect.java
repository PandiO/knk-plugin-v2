package net.knightsandkings.knk.paper.enchantment.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface EnchantmentEffect {
    String enchantmentId();

    boolean tryExecute(ItemStack weapon, Player attacker, LivingEntity target, int level);
}

package net.knightsandkings.knk.paper.enchantment.effects.impl;

import net.knightsandkings.knk.paper.enchantment.effects.SupportEnchantmentEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

public class ArmorRepairEffect extends SupportEnchantmentEffect {
    public ArmorRepairEffect(Plugin plugin) {
        super("armor_repair", plugin);
    }

    @Override
    protected void applyEffect(ItemStack item, Player player, int level) {
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armorContents) {
            if (armorPiece == null || armorPiece.getType().isAir() || armorPiece.getType().getMaxDurability() <= 0) {
                continue;
            }

            if (armorPiece.getItemMeta() instanceof Damageable damageableMeta) {
                damageableMeta.setDamage(0);
                armorPiece.setItemMeta(damageableMeta);
            }
        }

        player.getInventory().setArmorContents(armorContents);
        player.updateInventory();
        playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
    }
}

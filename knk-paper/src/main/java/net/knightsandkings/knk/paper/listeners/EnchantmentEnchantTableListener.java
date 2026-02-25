package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class EnchantmentEnchantTableListener implements Listener {
    private final EnchantmentRepository enchantmentRepository;

    public EnchantmentEnchantTableListener(EnchantmentRepository enchantmentRepository) {
        this.enchantmentRepository = enchantmentRepository;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (hasAnyCustomEnchantment(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (hasAnyCustomEnchantment(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private boolean hasAnyCustomEnchantment(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta != null && itemMeta.hasLore() ? itemMeta.getLore() : List.of();
        return enchantmentRepository.hasAnyEnchantment(lore).join();
    }
}

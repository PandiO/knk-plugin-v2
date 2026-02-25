package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.core.ports.enchantment.EnchantmentExecutor;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class EnchantmentCombatListener implements Listener {
    private final EnchantmentRepository enchantmentRepository;
    private final EnchantmentExecutor enchantmentExecutor;
    private final boolean disableForCreative;

    public EnchantmentCombatListener(
            EnchantmentRepository enchantmentRepository,
            EnchantmentExecutor enchantmentExecutor,
            boolean disableForCreative
    ) {
        this.enchantmentRepository = enchantmentRepository;
        this.enchantmentExecutor = enchantmentExecutor;
        this.disableForCreative = disableForCreative;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (disableForCreative && attacker.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) {
            return;
        }

        ItemMeta itemMeta = weapon.getItemMeta();
        List<String> lore = itemMeta != null && itemMeta.hasLore() ? itemMeta.getLore() : List.of();

        enchantmentRepository.getEnchantments(lore)
                .thenCompose(enchantments -> enchantmentExecutor.executeOnMeleeHit(
                        enchantments,
                        attacker.getUniqueId(),
                        target.getUniqueId(),
                        event.getDamage()
                ));
    }
}

package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.paper.tasks.LocationTaskHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Routes block right-clicks to active LocationSelection world tasks.
 */
public class WorldTaskLocationSelectionListener implements Listener {
    private final LocationTaskHandler locationTaskHandler;

    public WorldTaskLocationSelectionListener(LocationTaskHandler locationTaskHandler) {
        this.locationTaskHandler = locationTaskHandler;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (locationTaskHandler.onBlockRightClick(event.getPlayer(), event.getClickedBlock())) {
            event.setCancelled(true);
        }
    }
}
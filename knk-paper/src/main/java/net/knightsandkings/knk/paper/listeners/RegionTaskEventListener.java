package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.paper.events.OnRegionEnterEvent;
import net.knightsandkings.knk.paper.tasks.WgRegionIdTaskHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener for region-related events that dispatch to task handlers.
 * Wires OnRegionEnterEvent to the WgRegionId task handler.
 */
public class RegionTaskEventListener implements Listener {
    private final WgRegionIdTaskHandler wgRegionIdTaskHandler;

    public RegionTaskEventListener(WgRegionIdTaskHandler wgRegionIdTaskHandler) {
        this.wgRegionIdTaskHandler = wgRegionIdTaskHandler;
    }

    /**
     * When a player enters a region, dispatch to the WgRegionId handler if active.
     */
    @EventHandler
    public void onRegionEnter(OnRegionEnterEvent event) {
        Player player = event.getPlayer();
        String regionId = event.getRegionId();
        wgRegionIdTaskHandler.onRegionEnter(player, regionId);
    }
}

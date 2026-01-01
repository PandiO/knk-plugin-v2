package net.knightsandkings.knk.paper.tasks;

import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handler for WgRegionId field tasks.
 * Allows players to claim a WorldGuard region as the value for a field.
 * Listens for region entry events and completes the task when a region is claimed.
 */
public class WgRegionIdTaskHandler implements IWorldTaskHandler {
    private static final Logger LOGGER = Logger.getLogger(WgRegionIdTaskHandler.class.getName());
    private static final String FIELD_NAME = "WgRegionId";

    private final WorldTasksApi worldTasksApi;
    private final Plugin plugin;
    
    // Track active tasks: player -> taskId
    private final Map<Player, Integer> activeTasksByPlayer = new HashMap<>();

    public WgRegionIdTaskHandler(WorldTasksApi worldTasksApi, Plugin plugin) {
        this.worldTasksApi = worldTasksApi;
        this.plugin = plugin;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public void startTask(Player player, int taskId, String inputJson) {
        activeTasksByPlayer.put(player, taskId);
        player.sendMessage("§6[WorldTask] Enter a WorldGuard region to claim it as the value for this field.");
        player.sendMessage("§7[WorldTask] Task ID: " + taskId);
        LOGGER.info("Started WgRegionId task for player " + player.getName() + " (task " + taskId + ")");
    }

    @Override
    public boolean isHandling(Player player) {
        return activeTasksByPlayer.containsKey(player);
    }

    @Override
    public void cancel(Player player) {
        Integer taskId = activeTasksByPlayer.remove(player);
        if (taskId != null) {
            player.sendMessage("§c[WorldTask] Task cancelled.");
            LOGGER.info("Cancelled WgRegionId task for player " + player.getName() + " (task " + taskId + ")");
        }
    }

    @Override
    public Integer getTaskId(Player player) {
        return activeTasksByPlayer.get(player);
    }

    /**
     * Called when a player enters a region.
     * If the player is handling a WgRegionId task, captures the region ID and completes the task.
     * 
     * @param player The player entering the region
     * @param regionId The WorldGuard region ID
     */
    public void onRegionEnter(Player player, String regionId) {
        Integer taskId = activeTasksByPlayer.get(player);
        if (taskId == null) return;

        // Build output JSON as a simple string representation
        String outputJson = String.format(
            "{\"fieldName\":\"WgRegionId\",\"claimedRegionId\":\"%s\",\"claimedAt\":%d}",
            regionId.replace("\"", "\\\""),
            System.currentTimeMillis()
        );

        // Complete the task via API
        worldTasksApi.complete(taskId, outputJson)
            .thenAccept(completedTask -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    activeTasksByPlayer.remove(player);
                    player.sendMessage("§a[WorldTask] ✓ Task completed! Region " + regionId + " has been claimed.");
                    LOGGER.info("Completed WgRegionId task for player " + player.getName() 
                        + " (task " + taskId + ") with region: " + regionId);
                });
            })
            .exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c[WorldTask] Failed to complete task: " + ex.getMessage());
                    LOGGER.warning("Failed to complete WgRegionId task " + taskId + ": " + ex.getMessage());
                });
                return null;
            });
    }
}


package net.knightsandkings.knk.paper.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handler for WgRegionId field tasks.
 * Allows players to define a WorldGuard region using WorldEdit selection.
 * Supports selection-based region creation with validation and cleanup.
 */
public class WgRegionIdTaskHandler implements IWorldTaskHandler {
    private static final Logger LOGGER = Logger.getLogger(WgRegionIdTaskHandler.class.getName());
    private static final String FIELD_NAME = "WgRegionId";
    private static final String TEMP_REGION_PREFIX = "tempregion_worldtask_";

    private final WorldTasksApi worldTasksApi;
    private final Plugin plugin;
    
    // Track active tasks: player -> TaskContext
    private final Map<Player, TaskContext> activeTasksByPlayer = new HashMap<>();

    /**
     * Internal context for tracking task state
     */
    private static class TaskContext {
        final int taskId;
        final String inputJson;
        String parentRegionId;
        int priority;
        Map<Flag<?>, Object> flags;
        String createdRegionId;
        boolean paused;

        TaskContext(int taskId, String inputJson) {
            this.taskId = taskId;
            this.inputJson = inputJson;
            this.priority = 0; // default priority
            this.flags = new HashMap<>();
            this.paused = false;
        }
    }

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
        TaskContext context = new TaskContext(taskId, inputJson);
        
        // Parse input JSON for parent region, priority, flags if provided
        if (inputJson != null && !inputJson.isEmpty()) {
            try {
                JsonObject input = JsonParser.parseString(inputJson).getAsJsonObject();
                if (input.has("parentRegionId")) {
                    context.parentRegionId = input.get("parentRegionId").getAsString();
                }
                if (input.has("priority")) {
                    context.priority = input.get("priority").getAsInt();
                }
                // TODO: Parse flags from input if needed
            } catch (Exception e) {
                LOGGER.warning("Failed to parse input JSON for task " + taskId + ": " + e.getMessage());
            }
        }
        
        activeTasksByPlayer.put(player, context);
        
        // Equip WorldEdit wand and enable CUI selection (must run on main thread)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                session.setTool(ItemTypes.get(session.getWandItem()), new SelectionWand());
                session.dispatchCUISelection(BukkitAdapter.adapt(player));
                
                player.sendMessage("§6[WorldTask] Define a WorldGuard region using WorldEdit selection.");
                player.sendMessage("§7[WorldTask] Task ID: " + taskId);
                player.sendMessage("§eTip: Use '//sel poly' for polygonal selection, or '//sel cuboid' for cubic regions.");
                player.sendMessage("§eFor more info: https://minecraft-worldedit.fandom.com/wiki///sel");
                player.sendMessage("§aType 'save' in chat to confirm your selection, or 'cancel' to abort.");
                if (context.parentRegionId != null) {
                    player.sendMessage("§7Note: Selection must be inside parent region: " + context.parentRegionId);
                }
                
                LOGGER.info("Started WgRegionId task for player " + player.getName() + " (task " + taskId + ")");
            } catch (InvalidToolBindException e) {
                player.sendMessage("§c[WorldTask] Failed to equip WorldEdit wand: " + e.getMessage());
                LOGGER.warning("Failed to equip wand for task " + taskId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public boolean isHandling(Player player) {
        return activeTasksByPlayer.containsKey(player);
    }

    @Override
    public void cancel(Player player) {
        TaskContext context = activeTasksByPlayer.remove(player);
        if (context != null) {
            // Cleanup: remove temp region if created
            if (context.createdRegionId != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    cleanupTempRegion(player.getWorld(), context.createdRegionId);
                });
            }
            
            // Clear WorldEdit session
            WorldEdit.getInstance().getSessionManager().remove(BukkitAdapter.adapt(player));
            
            player.sendMessage("§c[WorldTask] Task cancelled.");
            LOGGER.info("Cancelled WgRegionId task for player " + player.getName() + " (task " + context.taskId + ")");
        }
    }

    @Override
    public Integer getTaskId(Player player) {
        TaskContext context = activeTasksByPlayer.get(player);
        return context != null ? context.taskId : null;
    }

    /**
     * Handle chat input from player during task.
     * Processes 'save', 'cancel', 'pause', 'resume' commands.
     * 
     * @param player The player
     * @param message The chat message
     * @return true if the message was handled and should be cancelled
     */
    public boolean onPlayerChat(Player player, String message) {
        TaskContext context = activeTasksByPlayer.get(player);
        if (context == null) return false;

        String cmd = message.trim().toLowerCase();
        
        if (cmd.equals("save")) {
            handleSave(player, context);
            return true;
        } else if (cmd.equals("cancel")) {
            cancel(player);
            return true;
        } else if (cmd.equals("pause") || cmd.equals("suspend")) {
            handlePause(player, context);
            return true;
        } else if (cmd.equals("resume")) {
            handleResume(player, context);
            return true;
        }
        
        return false;
    }

    /**
     * Handle save command: validate selection and create region
     */
    private void handleSave(Player player, TaskContext context) {
        if (context.paused) {
            player.sendMessage("§c[WorldTask] Task is paused. Type 'resume' to continue.");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                World world = player.getWorld();
                
                // Get WorldEdit selection
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                Region selection = session.getSelection(BukkitAdapter.adapt(world));
                
                if (selection == null) {
                    player.sendMessage("§c[WorldTask] No WorldEdit selection found! Please select a region first.");
                    return;
                }

                // Validate parent region containment if required
                if (context.parentRegionId != null) {
                    RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                    
                    if (regionManager != null) {
                        ProtectedRegion parentRegion = regionManager.getRegion(context.parentRegionId);
                        
                        if (parentRegion == null) {
                            player.sendMessage("§c[WorldTask] Parent region not found: " + context.parentRegionId);
                            return;
                        }
                        
                        if (!isSelectionInsideRegion(parentRegion, selection)) {
                            player.sendMessage("§c[WorldTask] Selection is not entirely inside required region: " + context.parentRegionId);
                            return;
                        }
                    }
                }

                // Create WorldGuard region from selection
                String tempRegionId = TEMP_REGION_PREFIX + context.taskId;
                ProtectedRegion region = createRegionFromSelection(selection, tempRegionId, context.priority);
                
                // Apply flags if any
                if (!context.flags.isEmpty()) {
                    for (Map.Entry<Flag<?>, Object> entry : context.flags.entrySet()) {
                        region.setFlag((Flag) entry.getKey(), entry.getValue());
                    }
                }

                // Register region in WorldGuard
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null) {
                    regionManager.addRegion(region);
                    context.createdRegionId = tempRegionId;
                    
                    // Clear WorldEdit session
                    WorldEdit.getInstance().getSessionManager().remove(BukkitAdapter.adapt(player));
                    
                    player.sendMessage("§a[WorldTask] Region created: " + tempRegionId);
                    player.sendMessage("§7Completing task...");
                    
                    // Complete task via API
                    completeTask(player, context, tempRegionId, world.getName());
                } else {
                    player.sendMessage("§c[WorldTask] Failed to access WorldGuard region manager.");
                }
                
            } catch (Exception e) {
                player.sendMessage("§c[WorldTask] Error creating region: " + e.getMessage());
                LOGGER.warning("Error in handleSave for task " + context.taskId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Handle pause command
     */
    private void handlePause(Player player, TaskContext context) {
        context.paused = true;
        player.sendMessage("§e[WorldTask] Task paused. Your selection is preserved.");
        player.sendMessage("§7Type 'resume' to continue, or 'cancel' to abort.");
        LOGGER.info("Paused WgRegionId task " + context.taskId + " for player " + player.getName());
    }

    /**
     * Handle resume command
     */
    private void handleResume(Player player, TaskContext context) {
        if (!context.paused) {
            player.sendMessage("§c[WorldTask] Task is not paused.");
            return;
        }
        
        context.paused = false;
        player.sendMessage("§a[WorldTask] Task resumed.");
        player.sendMessage("§7Type 'save' to confirm your selection.");
        LOGGER.info("Resumed WgRegionId task " + context.taskId + " for player " + player.getName());
    }

    /**
     * Complete the task via API
     */
    private void completeTask(Player player, TaskContext context, String regionId, String worldName) {
        // Build output JSON
        JsonObject output = new JsonObject();
        output.addProperty("fieldName", FIELD_NAME);
        output.addProperty("regionId", regionId);
        output.addProperty("createdAt", System.currentTimeMillis());
        output.addProperty("worldName", worldName);
        
        if (context.parentRegionId != null) {
            output.addProperty("parentRegionId", context.parentRegionId);
        }
        
        String outputJson = output.toString();

        // Complete the task via API
        worldTasksApi.complete(context.taskId, outputJson)
            .thenAccept(completedTask -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    activeTasksByPlayer.remove(player);
                    player.sendMessage("§a[WorldTask] ✓ Task completed! Region " + regionId + " has been created.");
                    LOGGER.info("Completed WgRegionId task for player " + player.getName() 
                        + " (task " + context.taskId + ") with region: " + regionId);
                });
            })
            .exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c[WorldTask] Failed to complete task: " + ex.getMessage());
                    player.sendMessage("§7The region was created but not saved. Cleaning up...");
                    cleanupTempRegion(player.getWorld(), regionId);
                    LOGGER.warning("Failed to complete WgRegionId task " + context.taskId + ": " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Check if a WorldEdit selection is completely inside a WorldGuard region.
     * Validates all polygon vertices at min and max Y.
     */
    private boolean isSelectionInsideRegion(ProtectedRegion parentRegion, Region selection) {
        if (selection instanceof Polygonal2DRegion) {
            Polygonal2DRegion poly = (Polygonal2DRegion) selection;
            
            for (BlockVector2 v2 : poly.getPoints()) {
                BlockVector3 v3Min = v2.toBlockVector3(poly.getMinimumY());
                BlockVector3 v3Max = v2.toBlockVector3(poly.getMaximumY());
                
                if (!parentRegion.contains(v3Min) || !parentRegion.contains(v3Max)) {
                    return false;
                }
            }
            return true;
        }
        
        // For other selection types, check min/max points
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        
        return parentRegion.contains(min) && parentRegion.contains(max);
    }

    /**
     * Create a ProtectedRegion from a WorldEdit selection
     */
    private ProtectedRegion createRegionFromSelection(Region selection, String id, int priority) {
        if (selection instanceof Polygonal2DRegion) {
            Polygonal2DRegion poly = (Polygonal2DRegion) selection;
            ProtectedPolygonalRegion region = new ProtectedPolygonalRegion(
                id,
                poly.getPoints(),
                poly.getMinimumY(),
                poly.getMaximumY()
            );
            region.setPriority(priority);
            return region;
        }
        
        // For other types, create a cuboid region from bounds
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        
        ProtectedRegion region = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
            id,
            min,
            max
        );
        region.setPriority(priority);
        return region;
    }

    /**
     * Cleanup temporary region if it's unused
     */
    private void cleanupTempRegion(World world, String regionId) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
            
            if (regionManager != null && regionId.startsWith(TEMP_REGION_PREFIX)) {
                // TODO: Check if region is linked to any entity before removing
                // For now, we remove all temp regions on cleanup
                regionManager.removeRegion(regionId, RemovalStrategy.REMOVE_CHILDREN);
                LOGGER.info("Cleaned up temporary region: " + regionId);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to cleanup temp region " + regionId + ": " + e.getMessage());
        }
    }

    /**
     * Called when a player enters a region (legacy support).
     * If the player is handling a WgRegionId task, captures the region ID and completes the task.
     * 
     * @param player The player entering the region
     * @param regionId The WorldGuard region ID
     * @deprecated Use selection-based creation instead
     */
    @Deprecated
    public void onRegionEnter(Player player, String regionId) {
        TaskContext context = activeTasksByPlayer.get(player);
        if (context == null) return;

        // Build output JSON
        String outputJson = String.format(
            "{\"fieldName\":\"WgRegionId\",\"claimedRegionId\":\"%s\",\"claimedAt\":%d}",
            regionId.replace("\"", "\\\""),
            System.currentTimeMillis()
        );

        // Complete the task via API
        worldTasksApi.complete(context.taskId, outputJson)
            .thenAccept(completedTask -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    activeTasksByPlayer.remove(player);
                    player.sendMessage("§a[WorldTask] ✓ Task completed! Region " + regionId + " has been claimed.");
                    LOGGER.info("Completed WgRegionId task for player " + player.getName() 
                        + " (task " + context.taskId + ") with region: " + regionId);
                });
            })
            .exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c[WorldTask] Failed to complete task: " + ex.getMessage());
                    LOGGER.warning("Failed to complete WgRegionId task " + context.taskId + ": " + ex.getMessage());
                });
                return null;
            });
    }
}


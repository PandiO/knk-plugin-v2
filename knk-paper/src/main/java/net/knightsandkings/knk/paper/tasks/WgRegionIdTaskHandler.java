package net.knightsandkings.knk.paper.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
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
 * Allows players to define a WorldGuard region by either:
 * 1. Creating a new region using WorldEdit selection
 * 2. Selecting an existing WorldGuard region
 * Supports both approaches with validation and cleanup.
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
        
        // Enable CUI selection for WorldEdit (must run on main thread)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                session.dispatchCUISelection(BukkitAdapter.adapt(player));
                
                player.sendMessage("§6[WorldTask] Define a WorldGuard region.");
                player.sendMessage("§7[WorldTask] Task ID: " + taskId);
                player.sendMessage("§eOption 1 - Create new region: Use '//sel poly' or '//sel cuboid', then type 'save'");
                player.sendMessage("§eOption 2 - Select existing: Type 'select {regionname}'");
                player.sendMessage("§7Or type 'cancel' to abort.");
                if (context.parentRegionId != null) {
                    player.sendMessage("§7Note: Region must be inside parent region: " + context.parentRegionId);
                }
                
                LOGGER.info("Started WgRegionId task for player " + player.getName() + " (task " + taskId + ")");
            } catch (Exception e) {
                player.sendMessage("§c[WorldTask] Failed to initialize WorldEdit session: " + e.getMessage());
                LOGGER.warning("Failed to initialize WorldEdit session for task " + taskId + ": " + e.getMessage());
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
     * Processes 'save', 'cancel', 'pause', 'resume', 'select {regionname}' commands.
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
        } else if (cmd.startsWith("select ")) {
            String regionName = message.trim().substring(7);
            handleSelect(player, context, regionName);
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
     * Handle select command: validate and select an existing region
     */
    private void handleSelect(Player player, TaskContext context, String regionName) {
        if (context.paused) {
            player.sendMessage("§c[WorldTask] Task is paused. Type 'resume' to continue.");
            return;
        }

        if (regionName == null || regionName.trim().isEmpty()) {
            player.sendMessage("§c[WorldTask] Please specify a region name: select {regionname}");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                World world = player.getWorld();
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
                
                if (regionManager == null) {
                    player.sendMessage("§c[WorldTask] Failed to access WorldGuard region manager.");
                    return;
                }
                
                ProtectedRegion region = regionManager.getRegion(regionName);
                
                if (region == null) {
                    player.sendMessage("§c[WorldTask] Region not found: " + regionName);
                    player.sendMessage("§7Available regions: " + String.join(", ", regionManager.getRegions().keySet()));
                    return;
                }
                
                // Validate parent region containment if required
                if (context.parentRegionId != null) {
                    ProtectedRegion parentRegion = regionManager.getRegion(context.parentRegionId);
                    
                    if (parentRegion == null) {
                        player.sendMessage("§c[WorldTask] Parent region not found: " + context.parentRegionId);
                        return;
                    }
                    
                    // Check if selected region is completely inside parent region
                    if (!isRegionInsideRegion(parentRegion, region)) {
                        player.sendMessage("§c[WorldTask] Region is not entirely inside required parent region: " + context.parentRegionId);
                        return;
                    }
                }
                
                player.sendMessage("§a[WorldTask] Region selected: " + regionName);
                player.sendMessage("§7Completing task...");
                
                // Clear WorldEdit session
                WorldEdit.getInstance().getSessionManager().remove(BukkitAdapter.adapt(player));
                
                // Complete task via API
                completeTask(player, context, regionName, world.getName());
                
            } catch (Exception e) {
                player.sendMessage("§c[WorldTask] Error selecting region: " + e.getMessage());
                LOGGER.warning("Error in handleSelect for task " + context.taskId + ": " + e.getMessage());
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
     * Check if a WorldGuard region is completely inside another WorldGuard region.
     * Compares the bounds of the child region against the parent region.
     */
    private boolean isRegionInsideRegion(ProtectedRegion parentRegion, ProtectedRegion childRegion) {
        BlockVector3 min = childRegion.getMinimumPoint();
        BlockVector3 max = childRegion.getMaximumPoint();
        
        // Check if all corners of child region are inside parent region
        return parentRegion.contains(min) && parentRegion.contains(max);
    }

    /**
     * Check if a WorldGuard region is completely inside another WorldGuard region.
     * Compares the bounds of the child region against the parent region.
     */
    private boolean isRegionInsideRegion(ProtectedRegion parentRegion, ProtectedRegion childRegion) {
        BlockVector3 min = childRegion.getMinimumPoint();
        BlockVector3 max = childRegion.getMaximumPoint();
        
        // Check if all corners of child region are inside parent region
        return parentRegion.contains(min) && parentRegion.contains(max);
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
     * Rename a WorldGuard region from its old name to a new name.
     * This is used to finalize temporary region names when an entity is successfully created.
     * Format for domain instance entities: "domain_{entity-id}"
     * 
     * @param oldRegionId The current/temporary region ID
     * @param newRegionId The final region ID
     * @return true if successful, false otherwise
     */
    public boolean renameRegion(String oldRegionId, String newRegionId) {
        if (oldRegionId == null || oldRegionId.trim().isEmpty() || 
            newRegionId == null || newRegionId.trim().isEmpty()) {
            LOGGER.warning("Cannot rename region: oldRegionId or newRegionId is null/empty");
            return false;
        }
        
        if (oldRegionId.equals(newRegionId)) {
            // Already the desired name
            return true;
        }
        
        try {
            // Find the world containing this region
            World world = findWorldByRegion(oldRegionId);
            if (world == null) {
                LOGGER.warning("Failed to rename region: could not find world containing region " + oldRegionId);
                return false;
            }
            
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                LOGGER.warning("Failed to rename region: could not access region manager for world " + world.getName());
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(oldRegionId);
            if (region == null) {
                LOGGER.warning("Failed to rename region: region not found: " + oldRegionId);
                return false;
            }
            
            // Check if target name already exists
            if (regionManager.getRegion(newRegionId) != null) {
                LOGGER.warning("Failed to rename region: target region name already exists: " + newRegionId);
                return false;
            }
            
            // Remove old region and create new one with updated name
            regionManager.removeRegion(oldRegionId);
            
            // Create new region with same properties but new ID
            ProtectedRegion newRegion;
            if (region instanceof ProtectedPolygonalRegion) {
                ProtectedPolygonalRegion polyRegion = (ProtectedPolygonalRegion) region;
                newRegion = new ProtectedPolygonalRegion(
                    newRegionId,
                    polyRegion.getPoints(),
                    polyRegion.getMinimumPoint().getBlockY(),
                    polyRegion.getMaximumPoint().getBlockY()
                );
            } else if (region instanceof com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion) {
                com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion cuboidRegion = 
                    (com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion) region;
                newRegion = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
                    newRegionId,
                    cuboidRegion.getMinimumPoint(),
                    cuboidRegion.getMaximumPoint()
                );
            } else {
                LOGGER.warning("Failed to rename region: unsupported region type");
                // Restore old region if we can't handle the type
                regionManager.addRegion(region);
                return false;
            }
            
            // Copy properties from old region
            newRegion.setPriority(region.getPriority());
            
            // Copy flags
            for (Flag<?> flag : region.getFlags().keySet()) {
                try {
                    Object value = region.getFlag(flag);
                    newRegion.setFlag((Flag) flag, value);
                } catch (Exception e) {
                    LOGGER.warning("Failed to copy flag " + flag.getName() + ": " + e.getMessage());
                }
            }
            
            // Add the new region
            regionManager.addRegion(newRegion);
            
            LOGGER.info("Successfully renamed region from " + oldRegionId + " to " + newRegionId);
            return true;
            
        } catch (Exception e) {
            LOGGER.warning("Failed to rename region from " + oldRegionId + " to " + newRegionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find the world that contains a specific region by name.
     * Searches all loaded worlds for the region.
     * 
     * @param regionId The region ID to search for
     * @return The world containing the region, or null if not found
     */
    private World findWorldByRegion(String regionId) {
        try {
            for (World world : plugin.getServer().getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
                
                if (regionManager != null && regionManager.getRegion(regionId) != null) {
                    return world;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error searching for region " + regionId + ": " + e.getMessage());
        }
        return null;
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
        // TaskContext context = activeTasksByPlayer.get(player);
        // if (context == null) return;

        // // Build output JSON
        // String outputJson = String.format(
        //     "{\"fieldName\":\"WgRegionId\",\"claimedRegionId\":\"%s\",\"claimedAt\":%d}",
        //     regionId.replace("\"", "\\\""),
        //     System.currentTimeMillis()
        // );

        // // Complete the task via API
        // worldTasksApi.complete(context.taskId, outputJson)
        //     .thenAccept(completedTask -> {
        //         plugin.getServer().getScheduler().runTask(plugin, () -> {
        //             activeTasksByPlayer.remove(player);
        //             player.sendMessage("§a[WorldTask] ✓ Task completed! Region " + regionId + " has been claimed.");
        //             LOGGER.info("Completed WgRegionId task for player " + player.getName() 
        //                 + " (task " + context.taskId + ") with region: " + regionId);
        //         });
        //     })
        //     .exceptionally(ex -> {
        //         plugin.getServer().getScheduler().runTask(plugin, () -> {
        //             player.sendMessage("§c[WorldTask] Failed to complete task: " + ex.getMessage());
        //             LOGGER.warning("Failed to complete WgRegionId task " + context.taskId + ": " + ex.getMessage());
        //         });
        //         return null;
        //     });
    }
}


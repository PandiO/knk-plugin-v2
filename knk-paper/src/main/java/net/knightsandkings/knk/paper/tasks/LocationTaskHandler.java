package net.knightsandkings.knk.paper.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handler for Location field tasks.
 * Allows admins to capture a player's current position (x, y, z, yaw, pitch)
 * by typing 'save' in chat during the task.
 */
public class LocationTaskHandler implements IWorldTaskHandler {
    private static final Logger LOGGER = Logger.getLogger(LocationTaskHandler.class.getName());
    private static final String FIELD_NAME = "Location";

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
        boolean paused;
        Location capturedLocation;  // Store for potential retry validation

        TaskContext(int taskId, String inputJson) {
            this.taskId = taskId;
            this.inputJson = inputJson;
            this.paused = false;
            this.capturedLocation = null;
        }
    }

    public LocationTaskHandler(WorldTasksApi worldTasksApi, Plugin plugin) {
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
        activeTasksByPlayer.put(player, context);

        player.sendMessage("§6[WorldTask] Capture your current location.");
        player.sendMessage("§7[WorldTask] Task ID: " + taskId);
        player.sendMessage("§eType 'save' to capture your current position (x, y, z, yaw, pitch)");
        player.sendMessage("§eType 'pause' to temporarily pause the task");
        player.sendMessage("§eType 'resume' to continue after pausing");
        player.sendMessage("§7Or type 'cancel' to abort.");

        LOGGER.info("Started Location task for player " + player.getName() + " (task " + taskId + ")");
    }

    @Override
    public boolean isHandling(Player player) {
        return activeTasksByPlayer.containsKey(player);
    }

    @Override
    public void cancel(Player player) {
        TaskContext context = activeTasksByPlayer.remove(player);
        if (context != null) {
            player.sendMessage("§c[WorldTask] Task cancelled.");
            LOGGER.info("Cancelled Location task for player " + player.getName() + " (task " + context.taskId + ")");
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
     * Handle save command: capture player location and complete task
     */
    private void handleSave(Player player, TaskContext context) {
        if (context.paused) {
            player.sendMessage("§c[WorldTask] Task is paused. Type 'resume' to continue.");
            return;
        }

        try {
            Location location = player.getLocation();
            
            // Validate location
            if (location == null || location.getWorld() == null) {
                player.sendMessage("§c[WorldTask] Error: Invalid location. Please try again.");
                LOGGER.warning("Invalid location captured for task " + context.taskId);
                return;
            }
            
            // Capture position and rotation
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            String worldName = location.getWorld().getName();
            
            // Store for validation if needed
            context.capturedLocation = location;
            
            player.sendMessage("§a[WorldTask] Location captured!");
            player.sendMessage(String.format("§7Position: (%.2f, %.2f, %.2f)", x, y, z));
            player.sendMessage(String.format("§7Rotation: yaw=%.2f, pitch=%.2f", yaw, pitch));
            player.sendMessage("§7World: " + worldName);
            player.sendMessage("§7Completing task...");
            
            // Complete task via API
            completeTask(player, context, x, y, z, yaw, pitch, worldName);
            
        } catch (Exception e) {
            player.sendMessage("§c[WorldTask] Error capturing location: " + e.getMessage());
            LOGGER.warning("Error in handleSave for task " + context.taskId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle pause command
     */
    private void handlePause(Player player, TaskContext context) {
        context.paused = true;
        player.sendMessage("§e[WorldTask] Task paused.");
        player.sendMessage("§7Type 'resume' to continue, or 'cancel' to abort.");
        LOGGER.info("Paused Location task " + context.taskId + " for player " + player.getName());
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
        player.sendMessage("§7Type 'save' to capture your current position.");
        LOGGER.info("Resumed Location task " + context.taskId + " for player " + player.getName());
    }

    /**
     * Complete the task via API
     */
    private void completeTask(Player player, TaskContext context, double x, double y, double z, 
                              float yaw, float pitch, String worldName) {
        // Build output JSON with location data
        JsonObject output = new JsonObject();
        output.addProperty("fieldName", FIELD_NAME);
        output.addProperty("x", x);
        output.addProperty("y", y);
        output.addProperty("z", z);
        output.addProperty("yaw", yaw);
        output.addProperty("pitch", pitch);
        output.addProperty("worldName", worldName);
        output.addProperty("capturedAt", System.currentTimeMillis());
        
        String outputJson = output.toString();

        // Complete the task via API
        worldTasksApi.complete(context.taskId, outputJson)
            .thenAccept(completedTask -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    activeTasksByPlayer.remove(player);
                    player.sendMessage("§a[WorldTask] ✓ Task completed! Location saved.");
                    LOGGER.info("Completed Location task for player " + player.getName() 
                        + " (task " + context.taskId + ") at position: (" + x + ", " + y + ", " + z + ")");
                });
            })
            .exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c[WorldTask] Failed to complete task: " + ex.getMessage());
                    LOGGER.warning("Failed to complete Location task " + context.taskId + ": " + ex.getMessage());
                });
                return null;
            });
    }
}

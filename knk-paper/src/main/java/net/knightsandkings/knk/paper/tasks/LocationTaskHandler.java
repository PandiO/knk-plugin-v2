package net.knightsandkings.knk.paper.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.knightsandkings.knk.core.domain.validation.ValidationResult;
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
            
            // NEW: Validate against rules from InputJson
            player.sendMessage("§7Validating location...");
            ValidationResult validation = validateLocation(player, location, context);
            if (!validation.isValid()) {
                player.sendMessage("§c✗ Validation Failed:");
                player.sendMessage("§c" + validation.getMessage());
                player.sendMessage("§e");
                if (validation.isBlocking()) {
                    player.sendMessage("§eTask not completed. Please move to a valid location and type 'save' again.");
                    player.sendMessage("§eOr type 'cancel' to abort the task.");
                    player.sendMessage("§eIf you believe this is an error, contact a developer.");
                    return; // Block completion
                } else {
                    player.sendMessage("§eWarning only - proceeding with save.");
                }
            }
            
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
     * Complete the task via API.
     * First creates a Location entity via API, then completes the task with the location ID.
     */
    private void completeTask(Player player, TaskContext context, double x, double y, double z, 
                              float yaw, float pitch, String worldName) {
        // Step 1: Create Location entity via API
        createLocationAndCompleteTask(player, context, x, y, z, yaw, pitch, worldName);
    }

    /**
     * Create Location entity via API and complete the task.
     * The Location API must return the created location with its ID.
     */
    private void createLocationAndCompleteTask(Player player, TaskContext context, double x, double y, double z,
                                                float yaw, float pitch, String worldName) {
        // Build Location DTO for API creation
        JsonObject locationDto = new JsonObject();
        locationDto.addProperty("name", "Location");
        locationDto.addProperty("x", x);
        locationDto.addProperty("y", y);
        locationDto.addProperty("z", z);
        locationDto.addProperty("yaw", yaw);
        locationDto.addProperty("pitch", pitch);
        locationDto.addProperty("world", worldName);

        // Call API to create Location (assuming worldTasksApi has a location creation method)
        // For now, we'll complete the task with the raw data and let the backend handle it
        // This is a fallback if the plugin doesn't have direct Location API access
        completeTaskWithLocationData(player, context, x, y, z, yaw, pitch, worldName);
    }

    /**
     * Complete the task with location data.
     * The backend will process this and create/update the Location entity.
     */
    private void completeTaskWithLocationData(Player player, TaskContext context, double x, double y, double z,
                                               float yaw, float pitch, String worldName) {
        // Build output JSON with location data
        // The backend workflow finalization process will handle creating the Location entity
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

        // Validate again before completing (safety net)
        Location validationLocation = context.capturedLocation;
        if (validationLocation == null) {
            validationLocation = new Location(player.getWorld(), x, y, z, yaw, pitch);
        }

        ValidationResult validation = validateLocation(player, validationLocation, context);
        if (!validation.isValid() && validation.isBlocking()) {
            player.sendMessage("§c✗ Validation Failed:");
            player.sendMessage("§c" + validation.getMessage());
            player.sendMessage("§eTask not completed. Please move to a valid location and type 'save' again.");
            player.sendMessage("§eOr type 'cancel' to abort the task.");
            return;
        }

        // Complete the task via API
        // Backend will extract this data and create Location entity during finalization
        worldTasksApi.complete(context.taskId, outputJson)
            .thenAccept(completedTask -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    activeTasksByPlayer.remove(player);
                    player.sendMessage("§a[WorldTask] ✓ Task completed! Location captured.");
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

    /**
     * Validate location against validation rules from InputJson.
     * Executes all configured validation rules and returns the first failure.
     * 
     * @param player The player
     * @param location The captured location
     * @param context Task context containing InputJson with validation rules
     * @return ValidationResult indicating success or failure
     */
    private ValidationResult validateLocation(Player player, Location location, TaskContext context) {
        try {
            // Parse validation context from InputJson
            if (context.inputJson == null || context.inputJson.isEmpty()) {
                return ValidationResult.success("No validation configured");
            }
            
            JsonObject input = JsonParser.parseString(context.inputJson).getAsJsonObject();
            if (!input.has("validationContext")) {
                return ValidationResult.success("No validation configured");
            }
            
            JsonObject validationContext = input.getAsJsonObject("validationContext");
            if (!validationContext.has("validationRules")) {
                return ValidationResult.success("No validation rules");
            }
            
            JsonArray rules = validationContext.getAsJsonArray("validationRules");
            
            // Execute each validation rule
            for (JsonElement ruleElement : rules) {
                JsonObject rule = ruleElement.getAsJsonObject();
                String validationType = rule.get("validationType").getAsString();
                
                if ("LocationInsideRegion".equals(validationType)) {
                    ValidationResult result = validateLocationInsideRegion(player, location, rule);
                    if (!result.isValid()) {
                        return result; // Return first failure
                    }
                }
                // Add more validation types here as needed
            }
            
            return ValidationResult.success("All validations passed");
            
        } catch (Exception e) {
            LOGGER.warning("Validation error for task " + context.taskId + ": " + e.getMessage());
            e.printStackTrace();
            // Fail-open: Allow task to proceed if validation parsing fails
            return ValidationResult.success("Validation skipped due to error");
        }
    }

    /**
     * Validate that a location is inside a WorldGuard region.
     * 
     * @param player The player
     * @param location The location to validate
     * @param rule The validation rule configuration
     * @return ValidationResult
     */
    private ValidationResult validateLocationInsideRegion(Player player, Location location, JsonObject rule) {
        try {
            // Parse config
            JsonObject config = JsonParser.parseString(rule.get("configJson").getAsString()).getAsJsonObject();
            
            // Get parent region ID from dependency field value
            if (!rule.has("dependencyFieldValue") || rule.get("dependencyFieldValue").isJsonNull()) {
                return ValidationResult.success("No dependency value - validation skipped");
            }
            
            JsonElement depValue = rule.get("dependencyFieldValue");
            
            // Extract parent region ID from dependency (e.g., Town entity)
            String parentRegionId = extractParentRegionId(depValue, config);
            if (parentRegionId == null || parentRegionId.isEmpty()) {
                return ValidationResult.success("No parent region ID - validation skipped");
            }
            
            // Get WorldGuard region manager
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return ValidationResult.blockingFailure("WorldGuard region manager not available for this world");
            }
            
            // Get parent region
            ProtectedRegion parentRegion = regionManager.getRegion(parentRegionId);
            if (parentRegion == null) {
                String errorMsg = rule.get("errorMessage").getAsString();
                errorMsg = errorMsg.replace("{regionName}", parentRegionId);
                return ValidationResult.blockingFailure(errorMsg);
            }
            
            // Check if location is inside parent region
            BlockVector3 blockLoc = BlockVector3.at(location.getX(), location.getY(), location.getZ());
            boolean isInside = parentRegion.contains(blockLoc);
            
            if (!isInside) {
                String errorMsg = rule.get("errorMessage").getAsString();
                errorMsg = errorMsg.replace("{regionName}", parentRegion.getId());
                errorMsg = errorMsg.replace("{coordinates}", 
                    String.format("(%.2f, %.2f, %.2f)", location.getX(), location.getY(), location.getZ()));
                
                boolean isBlocking = rule.has("isBlocking") && rule.get("isBlocking").getAsBoolean();
                return new ValidationResult(false, errorMsg, isBlocking);
            }
            
            // Success
            player.sendMessage("§a✓ Location is inside region: " + parentRegion.getId());
            return ValidationResult.success("Location is inside region");
            
        } catch (Exception e) {
            LOGGER.warning("LocationInsideRegion validation error: " + e.getMessage());
            e.printStackTrace();
            // Fail-open
            return ValidationResult.success("Validation skipped due to error");
        }
    }

    /**
     * Extract parent region ID from dependency field value.
     * The dependency value could be a full entity object or just the region ID.
     * 
     * @param depValue The dependency field value from InputJson
     * @param config The validation rule config
     * @return The parent region ID, or null if not found
     */
    private String extractParentRegionId(JsonElement depValue, JsonObject config) {
        try {
            // Get the property path from config (e.g., "WgRegionId")
            String regionPath = config.has("regionPropertyPath") 
                ? config.get("regionPropertyPath").getAsString()
                : "WgRegionId";
            
            // If depValue is a JSON object (Town entity), extract the region property
            if (depValue.isJsonObject()) {
                JsonObject entity = depValue.getAsJsonObject();
                if (entity.has(regionPath)) {
                    JsonElement regionIdElement = entity.get(regionPath);
                    if (regionIdElement.isJsonPrimitive()) {
                        return regionIdElement.getAsString();
                    }
                }
                
                // Try lowercase variant
                String lowerPath = regionPath.toLowerCase();
                if (entity.has(lowerPath)) {
                    return entity.get(lowerPath).getAsString();
                }
            }
            
            // If depValue is just the region ID string
            if (depValue.isJsonPrimitive()) {
                return depValue.getAsString();
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.warning("Error extracting parent region ID: " + e.getMessage());
            return null;
        }
    }
}

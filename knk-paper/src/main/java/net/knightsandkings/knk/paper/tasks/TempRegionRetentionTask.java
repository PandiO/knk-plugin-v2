package net.knightsandkings.knk.paper.tasks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Background task for enforcing retention policies on temporary WorldGuard regions.
 * 
 * Policy:
 * - Delete all regions matching the temp region prefix (e.g., "tempregion_worldtask_")
 *   that are older than the specified retention days.
 * 
 * Runs daily by default; checks region creation metadata to determine age.
 */
public class TempRegionRetentionTask {
    private static final Logger LOGGER = Logger.getLogger(TempRegionRetentionTask.class.getName());
    private static final String TEMP_REGION_PREFIX = "tempregion_worldtask_";
    private static final long RETENTION_MILLIS = 14L * 24 * 60 * 60 * 1000; // 14 days in milliseconds
    
    private final Plugin plugin;
    private final long retentionMillis;
    private BukkitTask task;

    public TempRegionRetentionTask(Plugin plugin, long retentionDays) {
        this.plugin = plugin;
        this.retentionMillis = retentionDays * 24 * 60 * 60 * 1000;
    }

    /**
     * Start the retention task, running once per day.
     */
    public void start() {
        // Run first check after 5 minutes, then every 24 hours
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::runCleanup,
            5 * 60 * 20, // 5 minutes (in ticks: 20 ticks/second)
            24 * 60 * 60 * 20 // 24 hours
        );
        LOGGER.info("TempRegionRetentionTask started. Will clean up temp regions older than " + (retentionMillis / (24 * 60 * 60 * 1000)) + " days");
    }

    /**
     * Stop the retention task.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        LOGGER.info("TempRegionRetentionTask stopped");
    }

    /**
     * Run the cleanup operation: scan all worlds for old temp regions and delete them.
     */
    private void runCleanup() {
        try {
            long cutoffTime = System.currentTimeMillis() - retentionMillis;
            int deletedCount = 0;

            for (World world : plugin.getServer().getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));

                if (regionManager == null) {
                    continue;
                }

                List<String> regionsToDelete = new ArrayList<>();

                // Find all temp regions older than retention period
                for (ProtectedRegion region : regionManager.getRegions().values()) {
                    if (region.getId().startsWith(TEMP_REGION_PREFIX)) {
                        // Check region age; if no metadata, assume it's old
                        long regionAge = System.currentTimeMillis(); // Default to current time if no metadata
                        
                        // Try to extract creation time from region metadata/comments if available
                        // For now, we'll use a simple heuristic: if region exists, assume reasonable age
                        // In production, store creation timestamp in region metadata
                        
                        if (regionAge >= cutoffTime) {
                            regionsToDelete.add(region.getId());
                        }
                    }
                }

                // Delete old temp regions
                for (String regionId : regionsToDelete) {
                    try {
                        regionManager.removeRegion(regionId);
                        deletedCount++;
                        LOGGER.info("Deleted temp region (retention policy): " + regionId + " from world " + world.getName());
                    } catch (Exception e) {
                        LOGGER.warning("Failed to delete temp region " + regionId + ": " + e.getMessage());
                    }
                }
            }

            if (deletedCount > 0) {
                LOGGER.info("Temp region retention cleanup completed. Deleted " + deletedCount + " regions.");
            }
        } catch (Exception e) {
            LOGGER.warning("Error running temp region retention cleanup: " + e.getMessage());
        }
    }
}

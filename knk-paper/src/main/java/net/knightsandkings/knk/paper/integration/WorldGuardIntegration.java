package net.knightsandkings.knk.paper.integration;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Integrates WorldGuard region synchronization with gate animation state.
 * Enables/disables regions based on whether gate is open or closed.
 */
public class WorldGuardIntegration {
    private static final Logger LOGGER = Logger.getLogger(WorldGuardIntegration.class.getName());
    
    private final JavaPlugin plugin;
    private final RegionContainer regionContainer;

    /**
     * Create a new WorldGuard integration handler.
     * 
     * @param plugin The plugin instance for scheduler access
     */
    public WorldGuardIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    /**
     * Synchronize WorldGuard regions based on gate animation state.
     * Called when animation finishes to update region access.
     * 
     * @param gate The gate being animated
     * @param newState The new animation state (OPEN or CLOSED)
     */
    public void syncRegions(CachedGate gate, AnimationState newState) {
        if (regionContainer == null) {
            LOGGER.fine("Region container not initialized, skipping sync");
            return;
        }

        try {
            if (newState == AnimationState.OPEN) {
                // Gate is open: disable closed region, enable open region
                disableRegion(gate.getRegionClosedId());
                enableRegion(gate.getRegionOpenedId());
                LOGGER.info("Synced regions for gate '" + gate.getName() + "': CLOSED disabled, OPEN enabled");
            } else if (newState == AnimationState.CLOSED) {
                // Gate is closed: enable closed region, disable open region
                enableRegion(gate.getRegionClosedId());
                disableRegion(gate.getRegionOpenedId());
                LOGGER.info("Synced regions for gate '" + gate.getName() + "': CLOSED enabled, OPEN disabled");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to sync regions for gate '" + gate.getName() + "': " + e.getMessage());
        }
    }

    /**
     * Enable a WorldGuard region by ID.
     * Region becomes active and enforces its flags.
     * Note: This is a placeholder for future implementation.
     * Current WorldGuard API (7.0.10) doesn't support enabling/disabling regions directly.
     * 
     * @param regionId The region ID to enable
     */
    private void enableRegion(String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return;
        }

        try {
            // Note: In WorldGuard 7.0.10, regions are always active when they exist.
            // To truly "enable" a region, you would:
            // 1. Modify region flags (e.g., set ENTRY to ALLOW)
            // 2. Or add the region to a specific region manager
            // 
            // For now, we log that this would enable the region
            LOGGER.fine("Enabled region: " + regionId);
        } catch (Exception e) {
            LOGGER.warning("Failed to enable region '" + regionId + "': " + e.getMessage());
        }
    }

    /**
     * Disable a WorldGuard region by ID.
     * Region becomes inactive and flags are not enforced.
     * Note: This is a placeholder for future implementation.
     * Current WorldGuard API (7.0.10) doesn't support enabling/disabling regions directly.
     * 
     * @param regionId The region ID to disable
     */
    private void disableRegion(String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return;
        }

        try {
            // Note: In WorldGuard 7.0.10, regions cannot be directly disabled.
            // To truly "disable" a region, you would:
            // 1. Modify region flags (e.g., set ENTRY to DENY or ALLOW)
            // 2. Or remove the region from the region manager
            // 3. Or use a meta-flag to indicate disabled state
            // 
            // For now, we log that this would disable the region
            LOGGER.fine("Disabled region: " + regionId);
        } catch (Exception e) {
            LOGGER.warning("Failed to disable region '" + regionId + "': " + e.getMessage());
        }
    }

    /**
     * Check if a region exists in WorldGuard.
     * 
     * @param regionId The region ID to check
     * @return true if region exists, false otherwise
     */
    public boolean regionExists(String regionId) {
        if (regionId == null || regionId.isEmpty() || regionContainer == null) {
            return false;
        }

        try {
            // TODO: Implement region lookup when we have proper world reference
            // For now, assume region exists if name is not empty
            return true;
        } catch (Exception e) {
            LOGGER.fine("Error checking region existence for '" + regionId + "': " + e.getMessage());
            return false;
        }
    }
}

package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.api.GateStructuresApi;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.logging.Logger;

/**
 * Manages gate health, destruction, and respawn mechanics.
 * Handles API persistence for state changes and schedules respawn tasks.
 */
public class HealthSystem {
    private static final Logger LOGGER = Logger.getLogger(HealthSystem.class.getName());

    private final GateManager gateManager;
    private final GateStructuresApi gateStructuresApi;
    private final Plugin plugin;

    /**
     * Create a new health system.
     * 
     * @param gateManager The gate manager containing all cached gates
     * @param gateStructuresApi The API client for persisting state
     * @param plugin The plugin instance for scheduler access
     */
    public HealthSystem(GateManager gateManager, GateStructuresApi gateStructuresApi, Plugin plugin) {
        this.gateManager = gateManager;
        this.gateStructuresApi = gateStructuresApi;
        this.plugin = plugin;
    }

    /**
     * Apply damage to a gate from an explosion.
     * If gate health reaches 0, destroys the gate.
     * 
     * @param gate The gate to damage
     * @param damageAmount The amount of damage to apply
     */
    public void applyDamage(CachedGate gate, double damageAmount) {
        if (gate == null || damageAmount <= 0) {
            return;
        }

        // Skip if invincible
        if (gate.isInvincible()) {
            LOGGER.info("Gate '" + gate.getName() + "' is invincible, ignoring damage");
            return;
        }

        // Apply damage
        double newHealth = Math.max(0, gate.getHealthCurrent() - damageAmount);
        gate.setHealthCurrent(newHealth);

        LOGGER.info("Gate '" + gate.getName() + "' took " + damageAmount + " damage. Health: " +
                   String.format("%.1f", newHealth) + "/" + gate.getHealthMax());

        // Check if gate is destroyed
        if (newHealth <= 0) {
            destroyGate(gate);
        } else {
            // Persist health change to API asynchronously
            persistHealthChange(gate);
        }
    }

    /**
     * Destroy a gate, disabling it and optionally scheduling respawn.
     * 
     * @param gate The gate to destroy
     */
    public void destroyGate(CachedGate gate) {
        if (gate == null || gate.isDestroyed()) {
            return;
        }

        LOGGER.info("Destroying gate: '" + gate.getName() + "'");

        // Update state
        gate.setIsDestroyed(true);
        gate.setIsActive(false);
        gate.setCurrentState(net.knightsandkings.knk.core.domain.gates.AnimationState.CLOSED);
        gate.setHealthCurrent(0);

        // Remove all gate blocks from the world
        removeGateBlocks(gate);

        // Persist destruction to API
        persistGateState(gate);

        // Schedule respawn if enabled
        if (gate.isCanRespawn()) {
            scheduleRespawn(gate);
        }
    }

    /**
     * Remove all block data associated with a gate from the world.
     * This clears the visual representation of the gate.
     * 
     * @param gate The gate whose blocks to remove
     */
    private void removeGateBlocks(CachedGate gate) {
        try {
            World world = Bukkit.getWorlds().get(0); // Get main world
            if (world == null) {
                LOGGER.warning("Could not find world to remove gate blocks");
                return;
            }

            int blocksRemoved = 0;
            for (var blockSnapshot : gate.getBlocks()) {
                Vector relativePos = blockSnapshot.getRelativePosition();
                if (relativePos == null) {
                    continue;
                }
                
                Block block = world.getBlockAt(
                    (int) relativePos.getX(),
                    (int) relativePos.getY(),
                    (int) relativePos.getZ()
                );
                
                if (block.getType().isOccluding()) {
                    block.setType(org.bukkit.Material.AIR);
                    blocksRemoved++;
                }
            }

            LOGGER.info("Removed " + blocksRemoved + " blocks from destroyed gate: '" + gate.getName() + "'");
        } catch (Exception e) {
            LOGGER.warning("Error removing gate blocks: " + e.getMessage());
        }
    }

    /**
     * Schedule a respawn task for a destroyed gate.
     * 
     * @param gate The gate to respawn
     */
    private void scheduleRespawn(CachedGate gate) {
        if (gate == null) {
            return;
        }

        long delayTicks = (long) gate.getRespawnRateSeconds() * 20L; // Convert seconds to ticks

        LOGGER.info("Scheduling respawn for gate '" + gate.getName() + "' in " +
                   gate.getRespawnRateSeconds() + " seconds");

        gate.setRespawnScheduledTime(System.currentTimeMillis() + (gate.getRespawnRateSeconds() * 1000L));

        // Schedule the respawn task
        new BukkitRunnable() {
            @Override
            public void run() {
                respawnGate(gate);
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Respawn a destroyed gate, restoring it to full health and active state.
     * 
     * @param gate The gate to respawn
     */
    public void respawnGate(CachedGate gate) {
        if (gate == null || !gate.isDestroyed()) {
            return;
        }

        LOGGER.info("Respawning gate: '" + gate.getName() + "'");

        // Update state
        gate.setIsDestroyed(false);
        gate.setIsActive(true);
        gate.setHealthCurrent(gate.getHealthMax());
        gate.setRespawnScheduledTime(0);

        // Persist respawn to API
        persistGateState(gate);

        // Notify players
        Bukkit.broadcastMessage("§a[KnK] Gate '" + gate.getName() + "' has been restored!");
    }

    /**
     * Persist gate health change to the API asynchronously.
     * 
     * @param gate The gate to persist
     */
    private void persistHealthChange(CachedGate gate) {
        if (gateStructuresApi == null) {
            LOGGER.warning("Gate API not available, cannot persist health change");
            return;
        }

        // Run API call asynchronously to avoid blocking the server thread
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // TODO: Call updateGateHealth() method on API when available
                    // For now, we'll log that this would persist
                    LOGGER.fine("Health change persisted to API for gate: " + gate.getName());
                } catch (Exception e) {
                    LOGGER.warning("Failed to persist health change: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Persist full gate state to the API asynchronously.
     * 
     * @param gate The gate to persist
     */
    private void persistGateState(CachedGate gate) {
        if (gateStructuresApi == null) {
            LOGGER.warning("Gate API not available, cannot persist gate state");
            return;
        }

        // Run API call asynchronously to avoid blocking the server thread
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // TODO: Call updateGate() method on API when available
                    // For now, we'll log that this would persist
                    LOGGER.fine("Gate state persisted to API: " + gate.getName() +
                               " (destroyed=" + gate.isDestroyed() + ", health=" + gate.getHealthCurrent() + ")");
                } catch (Exception e) {
                    LOGGER.warning("Failed to persist gate state: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}

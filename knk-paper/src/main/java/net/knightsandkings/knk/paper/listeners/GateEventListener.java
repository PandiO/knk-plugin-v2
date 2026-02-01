package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Gate event listener handling block break, explosions, and player interactions.
 * Prevents damage to gate blocks and manages gate health.
 */
public class GateEventListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(GateEventListener.class.getName());

    private final GateManager gateManager;

    public GateEventListener(GateManager gateManager) {
        this.gateManager = gateManager;
    }

    /**
     * Handle block break attempts on gate blocks.
     * Prevents breaking gate blocks unless player has admin permission.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if block is part of any gate
        CachedGate gate = findGateContainingBlock(block);
        if (gate == null) {
            return;
        }

        // Allow admins to break gate blocks
        if (player.hasPermission("knk.gate.admin")) {
            LOGGER.info("Admin " + player.getName() + " broke gate block at " + block.getLocation());
            return;
        }

        // Prevent breaking gate blocks
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You cannot break gate blocks. Gate: " + gate.getName());
    }

    /**
     * Handle explosions that might damage gate blocks.
     * Apply damage to gate if not invincible, or cancel explosion if invincible.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Set<Block> blocksToRemove = new HashSet<>();

        // Check each block affected by explosion
        for (Block block : event.blockList()) {
            CachedGate gate = findGateContainingBlock(block);
            if (gate == null) {
                continue;
            }

            if (gate.isInvincible()) {
                // Invincible gate: cancel damage to this block
                blocksToRemove.add(block);
            } else {
                // Vulnerable gate: apply damage
                double damageAmount = 10.0; // Configurable damage amount
                gate.setHealthCurrent(Math.max(0, gate.getHealthCurrent() - damageAmount));

                LOGGER.info("Gate " + gate.getName() + " damaged by explosion. Health: " + 
                           gate.getHealthCurrent() + "/" + gate.getHealthMax());

                if (gate.getHealthCurrent() <= 0) {
                    // Gate is destroyed
                    destroyGate(gate);
                    blocksToRemove.add(block);
                } else {
                    // Gate survives, but block is damaged
                    blocksToRemove.add(block);
                }
            }
        }

        // Remove protected blocks from explosion
        event.blockList().removeAll(blocksToRemove);
    }

    /**
     * Handle player interact events on gate blocks.
     * Optional: Can be used to trigger gate open/close with right-click.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        CachedGate gate = findGateContainingBlock(clickedBlock);
        if (gate == null) {
            return;
        }

        // Right-click to toggle gate
        if (event.getAction().isRightClick()) {
            Player player = event.getPlayer();

            // Check permission
            if (!player.hasPermission("knk.gate.open.*") && 
                !player.hasPermission("knk.gate.close.*")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to interact with this gate.");
                event.setCancelled(true);
                return;
            }

            // Toggle gate state
            // Note: This is optional functionality and can be enabled/disabled per configuration
            // For now, we'll log the interaction but not toggle automatically
            LOGGER.fine("Player " + player.getName() + " interacted with gate: " + gate.getName());
        }
    }

    /**
     * Find gate containing the given block.
     * Returns null if block is not part of any gate.
     */
    private CachedGate findGateContainingBlock(Block block) {
        // This is a simple O(n*m) implementation
        // In production, consider a spatial index for better performance
        
        for (CachedGate gate : gateManager.getAllGates().values()) {
            if (!gate.isActive()) {
                continue;
            }

            // Check if block is part of this gate's blocks
            // Note: This checks the anchor point + geometry, not current frame position
            // For animated gates, we'd need to check the current animation frame
            // For now, we check against stored gate block snapshots

            if (gate.getBlocks() != null && !gate.getBlocks().isEmpty()) {
                // Check if block location matches any gate block
                // This is simplified - in production you'd want better collision detection
                if (isBlockPartOfGate(block, gate)) {
                    return gate;
                }
            }
        }

        return null;
    }

    /**
     * Check if a block is part of a gate's structure.
     * Simplified implementation - checks stored block snapshots.
     */
    private boolean isBlockPartOfGate(Block block, CachedGate gate) {
        // This is a placeholder implementation
        // In a full implementation, you would:
        // 1. Calculate gate's current block positions based on animation state
        // 2. Check if the block matches any current position
        // 3. Use spatial indexing for performance
        
        return false; // Placeholder
    }

    /**
     * Handle gate destruction.
     */
    private void destroyGate(CachedGate gate) {
        gate.setIsDestroyed(true);
        gate.setIsActive(false);

        LOGGER.warning("Gate " + gate.getName() + " has been destroyed!");

        // TODO: Persist destruction to API
        // TODO: Schedule respawn if gate has respawn enabled
    }
}

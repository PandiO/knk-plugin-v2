package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.BlockSnapshot;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateFrameCalculator;
import net.knightsandkings.knk.core.gates.GateManager;
import net.knightsandkings.knk.paper.integration.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bukkit runnable that handles gate animation on every server tick.
 * Iterates through all gates in OPENING or CLOSING state and updates their block positions.
 * Integrates with WorldGuard to sync regions when animation completes.
 */
public class GateAnimationTask extends BukkitRunnable {
    private static final Logger LOGGER = Logger.getLogger(GateAnimationTask.class.getName());
    
    // TPS threshold for lag detection
    private static final double LAG_TPS_THRESHOLD = 15.0;
    
    // Minimum time between lag checks (milliseconds)
    private static final long LAG_CHECK_INTERVAL = 1000;

    private static final double ENTITY_PUSH_RADIUS = 5.0;
    private static final int ENTITY_COLLISION_FRAMES_THRESHOLD = 2;

    private final GateManager gateManager;
    private final World world;
    private final Material fallbackMaterial;
    private final WorldGuardIntegration worldGuardIntegration;
    
    private long lastLagCheck = 0;
    private boolean isLagging = false;
    private final Set<Integer> emptySnapshotWarnings = new HashSet<>();

    /**
     * Create a new gate animation task.
     * 
     * @param gateManager The gate manager containing all cached gates
     * @param world The world to place blocks in
     * @param fallbackMaterial Fallback material if block data is corrupted
     * @param worldGuardIntegration WorldGuard integration for region sync
     */
    public GateAnimationTask(GateManager gateManager, World world, Material fallbackMaterial, 
                             WorldGuardIntegration worldGuardIntegration) {
        this.gateManager = gateManager;
        this.world = world;
        this.fallbackMaterial = fallbackMaterial != null ? fallbackMaterial : Material.STONE;
        this.worldGuardIntegration = worldGuardIntegration;
        LOGGER.info("[GateAnimation] Scheduled animation task for world '" + world.getName() + "'");
    }

    @Override
    public void run() {
        // Check for lag periodically
        checkServerLag();

        // Get all gates
        Map<Integer, CachedGate> gates = gateManager.getAllGates();

        for (CachedGate gate : gates.values()) {
            if (!gate.getWorldName().isBlank() && !gate.getWorldName().equals(world.getName())) {
                continue;
            }

            AnimationState state = gate.getCurrentState();

            // Only process gates that are animating
            if (state != AnimationState.OPENING && state != AnimationState.CLOSING) {
                emptySnapshotWarnings.remove(gate.getId());
                continue;
            }

            // Skip if gate is inactive or destroyed
            if (!gate.isActive() || gate.isDestroyed()) {
                LOGGER.warning("[GateAnimation] Skipping gate '" + gate.getName() + "' (ID: " + gate.getId()
                    + ") because it is " + (!gate.isActive() ? "inactive" : "destroyed") + ".");
                continue;
            }

            if (gate.getBlocks().isEmpty()) {
                if (emptySnapshotWarnings.add(gate.getId())) {
                    LOGGER.warning("[GateAnimation] Gate '" + gate.getName() + "' (ID: " + gate.getId()
                        + ") has no block snapshots. State will complete but no blocks can be animated.");
                }
            }

            // Calculate current frame based on elapsed time
            long currentTime = System.currentTimeMillis();
            long elapsedTicks = (currentTime - gate.getAnimationStartTime()) / 50; // 50ms per tick
            int currentFrame = (int) elapsedTicks;

            // Clamp frame to valid range
            int totalFrames = gate.getAnimationDurationTicks();
            
            if (state == AnimationState.CLOSING) {
                // Closing: count down from totalFrames to 0
                currentFrame = totalFrames - currentFrame;
                currentFrame = Math.max(0, Math.min(currentFrame, totalFrames));
            } else {
                // Opening: count up from 0 to totalFrames
                currentFrame = Math.max(0, Math.min(currentFrame, totalFrames));
            }

            // Update gate's current frame
            gate.setCurrentFrame(currentFrame);

            if (currentFrame == 0 || currentFrame == totalFrames || currentFrame % 20 == 0) {
                LOGGER.info("[GateAnimation] Gate '" + gate.getName() + "' (ID: " + gate.getId() + ") "
                    + state + " frame " + currentFrame + "/" + totalFrames + " in world '" + world.getName() + "'.");
            }

            // Check if animation should update this frame
            if (!GateFrameCalculator.shouldUpdateFrame(gate, currentFrame)) {
                continue;
            }

            // If lagging, skip to final position
            if (isLagging && currentFrame > totalFrames / 2) {
                LOGGER.fine("Server lagging, skipping to final position for gate: " + gate.getName());
                currentFrame = state == AnimationState.OPENING ? totalFrames : 0;
                gate.setCurrentFrame(currentFrame);
            }

            // Handle entity push before updating blocks
            handleEntityPush(gate, currentFrame);

            // Update all block positions for this frame
            updateGateBlocks(gate, currentFrame);

            // Check if animation is complete
            if (currentFrame >= totalFrames && state == AnimationState.OPENING) {
                finishOpening(gate);
            } else if (currentFrame <= 0 && state == AnimationState.CLOSING) {
                finishClosing(gate);
            }
        }
    }

    /**
     * Update all block positions for a gate at a specific frame.
     * 
     * @param gate The gate to update
     * @param frame The current animation frame
     */
    private void updateGateBlocks(CachedGate gate, int frame) {
        if (gate == null || gate.getBlocks() == null) {
            return;
        }

        int previousFrame = gate.getCurrentState() == AnimationState.OPENING
            ? Math.max(0, frame - Math.max(1, gate.getAnimationTickRate()))
            : Math.min(gate.getAnimationDurationTicks(), frame + Math.max(1, gate.getAnimationTickRate()));

        List<BlockPlacement> placements = new ArrayList<>();
        List<BlockPlacement> vacancies = new ArrayList<>();
        Set<Long> targetCells = new HashSet<>();

        for (BlockSnapshot block : gate.getBlocks()) {
            if (block == null) {
                continue;
            }

            Vector worldPos = GateFrameCalculator.calculateBlockPosition(gate, block, frame);
            Vector previousPosition = GateFrameCalculator.calculateBlockPosition(gate, block, previousFrame);

            if (worldPos != null) {
                if (!GateBlockPlacer.isChunkLoaded(world, worldPos)) {
                    LOGGER.fine("Gate " + gate.getName() + " is in unloaded chunk, pausing animation");
                    return;
                }
                placements.add(new BlockPlacement(worldPos, block.getBlockData()));
                targetCells.add(packCell(worldPos));
            }

            if (previousPosition != null) {
                vacancies.add(new BlockPlacement(previousPosition, block.getBlockData()));
            }
        }

        // Clear vacated cells first, but never a cell another block moves into this frame:
        // otherwise a later snapshot erases what an earlier one just placed.
        for (BlockPlacement vacancy : vacancies) {
            if (targetCells.contains(packCell(vacancy.position()))) {
                continue;
            }
            GateBlockPlacer.removeBlockIfMatches(world, vacancy.position(), vacancy.blockData(), fallbackMaterial);
        }

        for (BlockPlacement placement : placements) {
            GateBlockPlacer.placeBlockIfVacant(world, placement.position(), placement.blockData(), fallbackMaterial);
        }
    }

    private static long packCell(Vector position) {
        return (((long) position.getBlockX() & 0x3FFFFFFL) << 38)
            | (((long) position.getBlockY() & 0xFFFL) << 26)
            | ((long) position.getBlockZ() & 0x3FFFFFFL);
    }

    private record BlockPlacement(Vector position, String blockData) {
    }

    private void handleEntityPush(CachedGate gate, int currentFrame) {
        Vector anchor = gate.getAnchorPoint();
        if (anchor == null) {
            return;
        }

        Location origin = new Location(world, anchor.getX(), anchor.getY(), anchor.getZ());
        double radius = entitySearchRadius(gate);

        for (Entity entity : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (entity.isDead()) {
                continue;
            }

            int framesToCollision = CollisionPredictor.predictCollision(gate, entity, currentFrame);
            if (framesToCollision == 0) {
                // Already inside the blocks being rendered this frame; a push cannot save it.
                if (!EntityEvacuator.evacuate(entity, gate)) {
                    EntityPusher.pushEntity(entity, gate);
                }
            } else if (framesToCollision <= ENTITY_COLLISION_FRAMES_THRESHOLD) {
                EntityPusher.pushEntity(entity, gate);
            }
        }
    }

    private double entitySearchRadius(CachedGate gate) {
        int span = Math.max(gate.getGeometryWidth(), Math.max(gate.getGeometryHeight(), gate.getGeometryDepth()));
        Vector motion = gate.getMotionVector();
        double travel = motion != null ? motion.length() : 0.0;
        return Math.max(ENTITY_PUSH_RADIUS, span + travel);
    }

    /**
     * Finish opening animation for a gate.
     * Syncs WorldGuard regions based on new state.
     * 
     * @param gate The gate that finished opening
     */
    private void finishOpening(CachedGate gate) {
        gate.setCurrentState(AnimationState.OPEN);
        gate.setCurrentFrame(gate.getAnimationDurationTicks());

        LOGGER.info("Gate " + gate.getName() + " finished opening");
        gateManager.notifyAnimationCompleted(gate.getId(), AnimationState.OPEN);

        // Sync WorldGuard regions
        if (worldGuardIntegration != null) {
            worldGuardIntegration.syncRegions(gate, AnimationState.OPEN, world);
        }
    }

    /**
     * Finish closing animation for a gate.
     * Syncs WorldGuard regions based on new state.
     * 
     * @param gate The gate that finished closing
     */
    private void finishClosing(CachedGate gate) {
        gate.setCurrentState(AnimationState.CLOSED);
        gate.setCurrentFrame(0);

        // Ensure all gate blocks are placed at closed position
        for (BlockSnapshot block : gate.getBlocks()) {
            if (block == null) {
                continue;
            }

            Vector worldPos = GateFrameCalculator.calculateBlockPosition(gate, block, 0);
            GateBlockPlacer.placeBlock(world, worldPos, block.getBlockData(), fallbackMaterial);
        }

        LOGGER.info("Gate " + gate.getName() + " finished closing");
        gateManager.notifyAnimationCompleted(gate.getId(), AnimationState.CLOSED);

        // Sync WorldGuard regions
        if (worldGuardIntegration != null) {
            worldGuardIntegration.syncRegions(gate, AnimationState.CLOSED, world);
        }
    }

    /**
     * Check server TPS to detect lag.
     * If TPS < 15, enable lag mode to skip animation frames.
     */
    private void checkServerLag() {
        long now = System.currentTimeMillis();
        
        if (now - lastLagCheck < LAG_CHECK_INTERVAL) {
            return;
        }

        lastLagCheck = now;

        try {
            // Get server TPS (Paper API)
            double tps = Bukkit.getTPS()[0]; // Last 1 minute average
            isLagging = tps < LAG_TPS_THRESHOLD;

            if (isLagging) {
                LOGGER.warning("Server lagging (TPS: " + String.format("%.2f", tps) + 
                              "), gate animations may skip frames");
            }
        } catch (Exception e) {
            // TPS API not available or error occurred
            isLagging = false;
        }
    }

    /**
     * Get the current lag status.
     * 
     * @return True if server is currently lagging
     */
    public boolean isLagging() {
        return isLagging;
    }
}

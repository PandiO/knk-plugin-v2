package net.knightsandkings.knk.core.gates;

import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.CachedGate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Manager for gate structures in the plugin.
 * Caches gates in memory and provides access to gate data and state management.
 * DTO loading and conversion is handled by adapters in the framework layer (knk-paper).
 */
public class GateManager {
    private static final Logger LOGGER = Logger.getLogger(GateManager.class.getName());

    private final Map<Integer, CachedGate> gateCache;

    public GateManager() {
        this.gateCache = new HashMap<>();
    }

    /**
     * Load all active gate structures from the API.
     * This method should be called during plugin startup.
     * DTO loading and conversion is delegated to framework adapters.
     *
     * @return CompletableFuture that completes when gates are reloaded
     */
    public CompletableFuture<Void> loadGatesFromApi() {
        // Actual loading is delegated to framework layer adapters
        // This method exists for backwards compatibility and can be called
        // by framework initialization code
        LOGGER.info("Gate loading delegated to framework adapters");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Cache a gate that has been loaded and converted by a framework adapter.
     * This is the primary entry point for gates created outside of this class.
     * Called by adapters in knk-paper after DTO conversion.
     *
     * @param gate The CachedGate instance to cache
     */
    public void cacheGate(CachedGate gate) {
        if (gate == null) {
            LOGGER.warning("Attempted to cache null gate");
            return;
        }
        
        gateCache.put(gate.getId(), gate);
        LOGGER.info("Cached gate: " + gate.getName() + " (ID: " + gate.getId() + 
                   ") with " + gate.getBlocks().size() + " blocks");
    }

    // === Public API for accessing gates ===

    /**
     * Get a cached gate by ID.
     *
     * @param id Gate ID
     * @return CachedGate or null if not found
     */
    public CachedGate getGate(int id) {
        return gateCache.get(id);
    }

    /**
     * Get a cached gate by name.
     *
     * @param name Gate name
     * @return CachedGate or null if not found
     */
    public CachedGate getGateByName(String name) {
        return gateCache.values().stream()
            .filter(gate -> gate.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get all cached gates.
     *
     * @return Map of gate ID to CachedGate
     */
    public Map<Integer, CachedGate> getAllGates() {
        return new HashMap<>(gateCache);
    }

    /**
     * Reload gates from the API.
     * Clears the cache and reloads all gates.
     *
     * @return CompletableFuture that completes when reload is done
     */
    public CompletableFuture<Void> reloadGates() {
        gateCache.clear();
        LOGGER.info("Cleared gate cache, reloading from API...");
        return loadGatesFromApi();
    }

    // === State Machine Methods ===

    /**
     * Open a gate, starting the opening animation.
     * 
     * @param gateId Gate ID
     * @return True if gate started opening, false if already open or animating
     */
    public boolean openGate(int gateId) {
        CachedGate gate = gateCache.get(gateId);
        
        if (gate == null) {
            LOGGER.warning("Cannot open gate: Gate ID " + gateId + " not found");
            return false;
        }

        // Check if gate is already open or opening
        AnimationState currentState = gate.getCurrentState();
        if (currentState == AnimationState.OPEN || currentState == AnimationState.OPENING) {
            LOGGER.fine("Gate " + gate.getName() + " is already open or opening");
            return false;
        }

        // Check if gate is active and not destroyed
        if (!gate.isActive() || gate.isDestroyed()) {
            LOGGER.warning("Cannot open gate " + gate.getName() + ": Gate is inactive or destroyed");
            return false;
        }

        // Start opening animation
        gate.setCurrentState(AnimationState.OPENING);
        gate.setCurrentFrame(0);
        gate.setAnimationStartTime(System.currentTimeMillis());

        LOGGER.info("Opening gate: " + gate.getName() + " (ID: " + gateId + ")");
        return true;
    }

    /**
     * Close a gate, starting the closing animation.
     * 
     * @param gateId Gate ID
     * @return True if gate started closing, false if already closed or animating
     */
    public boolean closeGate(int gateId) {
        CachedGate gate = gateCache.get(gateId);
        
        if (gate == null) {
            LOGGER.warning("Cannot close gate: Gate ID " + gateId + " not found");
            return false;
        }

        // Check if gate is already closed or closing
        AnimationState currentState = gate.getCurrentState();
        if (currentState == AnimationState.CLOSED || currentState == AnimationState.CLOSING) {
            LOGGER.fine("Gate " + gate.getName() + " is already closed or closing");
            return false;
        }

        // Check if gate is active
        if (!gate.isActive()) {
            LOGGER.warning("Cannot close gate " + gate.getName() + ": Gate is inactive");
            return false;
        }

        // Start closing animation
        gate.setCurrentState(AnimationState.CLOSING);
        gate.setCurrentFrame(gate.getAnimationDurationTicks());
        gate.setAnimationStartTime(System.currentTimeMillis());

        LOGGER.info("Closing gate: " + gate.getName() + " (ID: " + gateId + ")");
        return true;
    }

    /**
     * Toggle a gate between open and closed states.
     * 
     * @param gateId Gate ID
     * @return True if gate state was toggled
     */
    public boolean toggleGate(int gateId) {
        CachedGate gate = gateCache.get(gateId);
        
        if (gate == null) {
            return false;
        }

        AnimationState currentState = gate.getCurrentState();
        
        if (currentState == AnimationState.CLOSED) {
            return openGate(gateId);
        } else if (currentState == AnimationState.OPEN) {
            return closeGate(gateId);
        } else {
            LOGGER.fine("Cannot toggle gate " + gate.getName() + ": Gate is currently animating");
            return false;
        }
    }

    /**
     * Force a gate to a specific state immediately (skip animation).
     * Use with caution - mainly for admin commands or error recovery.
     * 
     * @param gateId Gate ID
     * @param isOpened Target state (true = open, false = closed)
     */
    public void forceGateState(int gateId, boolean isOpened) {
        CachedGate gate = gateCache.get(gateId);
        
        if (gate == null) {
            LOGGER.warning("Cannot force gate state: Gate ID " + gateId + " not found");
            return;
        }

        if (isOpened) {
            gate.setCurrentState(AnimationState.OPEN);
            gate.setCurrentFrame(gate.getAnimationDurationTicks());
        } else {
            gate.setCurrentState(AnimationState.CLOSED);
            gate.setCurrentFrame(0);
        }

        LOGGER.info("Forced gate " + gate.getName() + " to " + (isOpened ? "OPEN" : "CLOSED"));
    }

    /**
     * Check if a gate is currently animating.
     * 
     * @param gateId Gate ID
     * @return True if gate is opening or closing
     */
    public boolean isGateAnimating(int gateId) {
        CachedGate gate = gateCache.get(gateId);
        return gate != null && gate.isAnimating();
    }

    /**
     * Get the current animation progress of a gate.
     * 
     * @param gateId Gate ID
     * @return Progress from 0.0 (closed) to 1.0 (open), or -1.0 if gate not found
     */
    public double getGateProgress(int gateId) {
        CachedGate gate = gateCache.get(gateId);
        
        if (gate == null) {
            return -1.0;
        }

        int totalFrames = gate.getAnimationDurationTicks();
        if (totalFrames <= 0) {
            return 0.0;
        }

        return (double) gate.getCurrentFrame() / totalFrames;
    }
}

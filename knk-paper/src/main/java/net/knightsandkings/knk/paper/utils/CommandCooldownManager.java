package net.knightsandkings.knk.paper.utils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages command cooldowns to prevent spam and rate-limit expensive operations.
 * 
 * Thread-safe implementation using ConcurrentHashMap for cooldown tracking.
 */
public class CommandCooldownManager {
    private final ConcurrentHashMap<String, Long> cooldowns;
    private final Logger logger;
    
    public CommandCooldownManager(Logger logger) {
        this.cooldowns = new ConcurrentHashMap<>();
        this.logger = logger;
    }
    
    /**
     * Check if a player can execute a command, respecting cooldown.
     * 
     * @param playerId The player UUID
     * @param commandKey The command identifier (e.g., "account.create", "account.link.generate")
     * @param cooldownSeconds Cooldown duration in seconds
     * @return true if command can execute, false if still on cooldown
     */
    public boolean canExecute(UUID playerId, String commandKey, int cooldownSeconds) {
        String key = playerId.toString() + ":" + commandKey;
        Long lastExecution = cooldowns.get(key);
        
        if (lastExecution == null) {
            return true;
        }
        
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - lastExecution) / 1000;
        
        return elapsedSeconds >= cooldownSeconds;
    }
    
    /**
     * Get remaining cooldown time in seconds.
     * 
     * @param playerId The player UUID
     * @param commandKey The command identifier
     * @param cooldownSeconds Cooldown duration in seconds
     * @return Remaining seconds, or 0 if no cooldown active
     */
    public int getRemainingCooldown(UUID playerId, String commandKey, int cooldownSeconds) {
        String key = playerId.toString() + ":" + commandKey;
        Long lastExecution = cooldowns.get(key);
        
        if (lastExecution == null) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - lastExecution) / 1000;
        int remaining = (int) (cooldownSeconds - elapsedSeconds);
        
        return Math.max(0, remaining);
    }
    
    /**
     * Record command execution timestamp.
     * 
     * @param playerId The player UUID
     * @param commandKey The command identifier
     */
    public void recordExecution(UUID playerId, String commandKey) {
        String key = playerId.toString() + ":" + commandKey;
        long now = System.currentTimeMillis();
        cooldowns.put(key, now);
        logger.fine("Recorded cooldown for " + playerId + " on " + commandKey + " at " + now);
    }
    
    /**
     * Clear all cooldowns for a player (e.g., on logout).
     * 
     * @param playerId The player UUID
     */
    public void clearPlayerCooldowns(UUID playerId) {
        String prefix = playerId.toString() + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
        logger.fine("Cleared all cooldowns for player " + playerId);
    }
    
    /**
     * Reset a specific cooldown for a player.
     * 
     * @param playerId The player UUID
     * @param commandKey The command identifier
     */
    public void resetCooldown(UUID playerId, String commandKey) {
        String key = playerId.toString() + ":" + commandKey;
        cooldowns.remove(key);
        logger.fine("Reset cooldown for " + playerId + " on " + commandKey);
    }
    
    /**
     * Clean up expired cooldowns to prevent memory bloat.
     * Should be called periodically (e.g., every 5 minutes).
     * 
     * @param maxAgeSeconds Remove cooldowns older than this (e.g., 3600 = 1 hour)
     */
    public void cleanup(int maxAgeSeconds) {
        long now = System.currentTimeMillis();
        long maxAgeMillis = maxAgeSeconds * 1000L;
        final int[] removedCount = {0}; // Use array to make it effectively final
        
        cooldowns.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue()) > maxAgeMillis;
            if (expired) removedCount[0]++;
            return expired;
        });
        
        if (removedCount[0] > 0) {
            logger.info("Cleaned up " + removedCount[0] + " expired cooldowns");
        }
    }
}

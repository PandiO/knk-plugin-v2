package net.knightsandkings.knk.paper.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.ibm.icu.util.Region;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.knightsandkings.knk.core.regions.RegionTransitionDecision;
import net.knightsandkings.knk.core.regions.RegionDomainResolver;
import net.knightsandkings.knk.core.regions.RegionTransitionService;
import net.knightsandkings.knk.paper.events.OnRegionEnterEvent;
import net.knightsandkings.knk.paper.events.OnRegionLeaveEvent;

public class WorldGuardRegionTracker {
    private final RegionContainer regionContainer;
    private final RegionQuery regionQuery;
    private final RegionTransitionService transitionService;
    private final RegionDomainResolver regionResolver;
    private final Executor lookupExecutor;
    private final Logger logger;
    private final boolean enableConsoleLogging;

    private final Map<UUID, Set<String>> regionsByPlayer = new HashMap<>();
    private final Map<String, Long> failedRegionLookups = new ConcurrentHashMap<>();  // Track failed lookups with timestamp
    private static final long FAILED_LOOKUP_COOLDOWN_MS = 5000;  // 5 second cooldown before retrying

    public WorldGuardRegionTracker(RegionTransitionService transitionService, RegionDomainResolver regionResolver, Executor lookupExecutor, Logger logger, boolean enableConsoleLogging) {
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.regionQuery = regionContainer.createQuery();
        this.transitionService = transitionService;
        this.regionResolver = regionResolver;
        this.lookupExecutor = lookupExecutor;
        this.logger = logger;
        this.enableConsoleLogging = enableConsoleLogging;
    }

    public WorldGuardRegionTracker(RegionTransitionService transitionService, RegionDomainResolver regionResolver, Executor lookupExecutor) {
        this(transitionService, regionResolver, lookupExecutor, null, false);
    }

    /**
     * @return RegionTransitionDecision if a region change occurred, otherwise null.
     */
    public RegionTransitionDecision handleMove(Player player, Location from, Location to) {
        if (player == null || to == null) {
            return null;
        }

        // Performance tweak: only check if the player has changed block or world
        // if (!hasChangedBlock(from, to)) {
        //     return null;
        // }

        UUID playerId = player.getUniqueId();
        Set<String> oldRegions = regionsByPlayer.getOrDefault(playerId, Collections.emptySet());
        Set<String> newRegions = getRegionNamesAt(to);
            Bukkit.getLogger().info("[KnK Tracker] " + player.getName() + " move: oldRegions=" + oldRegions + ", newRegions=" + newRegions);

        if (logger != null) {
            logger.info("[KnK Tracker] " + player.getName() + " move: oldRegions=" + oldRegions + ", newRegions=" + newRegions);
            
            // Verify district region ID distinctness
            if (!oldRegions.equals(newRegions)) {
                Set<String> entered = new HashSet<>(newRegions);
                entered.removeAll(oldRegions);
                Set<String> left = new HashSet<>(oldRegions);
                left.removeAll(newRegions);
                
                if (!entered.isEmpty() || !left.isEmpty()) {
                    logger.info("[KnK Tracker] " + player.getName() + " WG region change detected: entered=" + entered + ", left=" + left);
                    
                    // Fire RegionEnterEvent for each region entered
                    for (String regionId : entered) {
                        OnRegionEnterEvent enterEvent = new OnRegionEnterEvent(player, regionId);
                        Bukkit.getPluginManager().callEvent(enterEvent);
                    }
                    
                    // Fire RegionLeaveEvent for each region left
                    for (String regionId : left) {
                        OnRegionLeaveEvent leaveEvent = new OnRegionLeaveEvent(player, regionId);
                        Bukkit.getPluginManager().callEvent(leaveEvent);
                    }
                }
            }
        }

        if (newRegions.equals(oldRegions)) {
            if (logger != null) {
                logger.info("[KnK Tracker] " + player.getName() + " no region change, returning null");
            }
            return null;
        }

        // Fire-and-forget API prefetch off the main thread for any region IDs we haven't cached yet.
        // Use union of old and new so message building has complete context (e.g., same-town district→district).
        Set<String> lookupIds = new HashSet<>(newRegions);
        lookupIds.addAll(oldRegions);
        
        boolean needsLookup = needsLookup(lookupIds);
        if (logger != null) {
            logger.info("[KnK Tracker] " + player.getName() + " cache check: lookupIds=" + lookupIds + ", needsLookup=" + needsLookup);
        }
        
        if (needsLookup) {
            if (logger != null) {
                logger.info("[KnK Tracker] " + player.getName() + " starting async prefetch for: " + lookupIds);
            }
            
            // Update player regions now
            regionsByPlayer.put(playerId, newRegions);
            
            CompletableFuture.runAsync(() -> {
                if (logger != null) {
                    logger.info("[KnK Tracker] " + player.getName() + " async prefetch started for: " + lookupIds);
                }
                try {
                    regionResolver.resolveRegionsFromApi(lookupIds).get();  // Block until complete
                    if (logger != null) {
                        logger.info("[KnK Tracker] " + player.getName() + " async prefetch completed for: " + lookupIds);
                    }
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.severe("[KnK Tracker] " + player.getName() + " async prefetch FAILED: " + ex.getMessage());
                    }
                    // Record these regions as failed so we don't retry immediately
                    recordFailedLookup(lookupIds);
                }
            }, lookupExecutor)
                .exceptionally(ex -> {
                    if (logger != null) {
                        logger.severe("[KnK Tracker] " + player.getName() + " async prefetch task FAILED: " + ex.getMessage());
                    }
                    recordFailedLookup(lookupIds);
                    return null;
                });

            // Still allow movement even if metadata isn't cached yet (fallback behavior)
            // If player is already in a region on join, just allow it without messages
            if (logger != null) {
                logger.info("[KnK Tracker] " + player.getName() + " allowing movement (metadata fetch in progress)");
            }
            // Return null to allow movement without messages while metadata is fetching
            return null;
        }

        // Log region transitions if enabled (only when actually processing the transition)
        if (enableConsoleLogging && logger != null) {
            logRegionTransition(player, oldRegions, newRegions);
        }

        // Update player regions now that we're processing the transition
        regionsByPlayer.put(playerId, newRegions);
        
        Bukkit.getLogger().info("[KnK Tracker] " + player.getName() + " calling transitionService.handleRegionTransition");
        if (logger != null) {
            logger.info("[KnK Tracker] " + player.getName() + " calling transitionService.handleRegionTransition");
        }

        RegionTransitionDecision decision = transitionService.handleRegionTransition(playerId, oldRegions, newRegions);
        
        if (logger != null) {
            logger.info("[KnK Tracker] " + player.getName() + " decision: allowed=" + decision.isMovementAllowed() + 
                        ", message=" + decision.getMessage().orElse("(none)"));
        }
        
        return decision;
    }

    public void handleQuit(Player player) {
        if (player != null) {
            regionsByPlayer.remove(player.getUniqueId());
        }
    }

    /**
     * Called on player join to initialize their region set and pre-warm API cache
     * to avoid initial misleading leave-only messages.
     */
    public RegionTransitionDecision handleJoin(Player player) {
        if (player == null || player.getLocation() == null) return null;
        UUID playerId = player.getUniqueId();
        Set<String> current = getRegionNamesAt(player.getLocation());
        
        if (logger != null) {
            logger.info("[KnK Tracker] " + player.getName() + " JOIN: initial regions=" + current);
        }
        
        regionsByPlayer.put(playerId, current);
        
        if (!current.isEmpty()) {
            if (logger != null) {
                logger.info("[KnK Tracker] " + player.getName() + " JOIN: starting cache pre-warm for: " + current);
            }
            CompletableFuture.runAsync(() -> {
                if (logger != null) {
                    logger.info("[KnK Tracker] " + player.getName() + " JOIN: async pre-warm started");
                }
                regionResolver.resolveRegionsFromApi(current);
                if (logger != null) {
                    logger.info("[KnK Tracker] " + player.getName() + " JOIN: async pre-warm completed");
                }
            }, lookupExecutor)
                .exceptionally(ex -> {
                    if (logger != null) {
                        logger.severe("[KnK Tracker] " + player.getName() + " JOIN: pre-warm FAILED: " + ex.getMessage());
                    }
                    return null;
                });
        }
        
        if (enableConsoleLogging && logger != null && !current.isEmpty()) {
            for (String r : current) {
                logger.info("[KnK Region] " + player.getName() + " joined inside region: " + r);
            }
        }

        RegionTransitionDecision decision = transitionService.handleRegionTransition(playerId, Collections.emptySet(), current);
        return decision;
    }

    private boolean hasChangedBlock(Location from, Location to) {
        if (from == null || to == null) return true;
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || !Objects.equals(from.getWorld(), to.getWorld());
    }

    private Set<String> getRegionNamesAt(Location bukkitLocation) {
        if (bukkitLocation.getWorld() == null) {
            return Collections.emptySet();
        }

        com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(bukkitLocation);
        ApplicableRegionSet set = regionQuery.getApplicableRegions(wgLoc);
        if (logger != null && enableConsoleLogging) {
                    logger.info("[KnK WG] Querying regions at location: world=" + bukkitLocation.getWorld().getName() + 
                ", x=" + bukkitLocation.getX() + ", y=" + bukkitLocation.getY() + ", z=" + bukkitLocation.getZ() + ". Regions found: " + set.size());
        }

        Set<String> names = new HashSet<>();
        for (ProtectedRegion region : set) {
            names.add(region.getId());
            if (logger != null && enableConsoleLogging) {
                logger.info("[KnK WG] Region detected at location: id=" + region.getId() + 
                            ", type=" + region.getType() + ", priority=" + region.getPriority());
            }
        }
        
        if (logger != null && enableConsoleLogging && !names.isEmpty()) {
            logger.info("[KnK WG] Total regions at location: " + names);
        }
        
        return names;
    }

    private boolean needsLookup(Set<String> regionIds) {
        if (regionResolver == null || regionIds == null || regionIds.isEmpty()) {
            return false;
        }

        for (String id : regionIds) {
            // Check if this region was recently failed to avoid hammering the API
            Long lastFailedTime = failedRegionLookups.get(id);
            if (lastFailedTime != null && System.currentTimeMillis() - lastFailedTime < FAILED_LOOKUP_COOLDOWN_MS) {
                if (logger != null) {
                    logger.info("[KnK Tracker] Region '" + id + "' recently failed lookup, skipping retry");
                }
                continue;  // Skip lookup for this region during cooldown
            }
            
            boolean isDomain = regionResolver.getDomainByRegionId(id).isPresent();
            boolean cached = isDomain;
            
            if (logger != null) {
                logger.info("[KnK Tracker] Cache check for '" + id + "': domain=" + isDomain + ", cached=" + cached);
            }
            
            if (!cached) {
                if (logger != null) {
                    logger.info("[KnK Tracker] Region '" + id + "' NOT CACHED, lookup needed");
                }
                return true;
            }
        }
        
        if (logger != null) {
            logger.info("[KnK Tracker] All regions cached, no lookup needed");
        }
        return false;
    }
    
    /**
     * Mark a region ID as having failed lookup, to avoid repeated API calls.
     */
    private void recordFailedLookup(Set<String> regionIds) {
        long now = System.currentTimeMillis();
        for (String id : regionIds) {
            failedRegionLookups.put(id, now);
        }
    }

    private void logRegionTransition(Player player, Set<String> oldRegions, Set<String> newRegions) {
        Set<String> entered = new HashSet<>(newRegions);
        entered.removeAll(oldRegions);

        Set<String> left = new HashSet<>(oldRegions);
        left.removeAll(newRegions);

        for (String region : entered) {
            logger.info("[KnK Region] " + player.getName() + " entered region: " + region);
        }

        for (String region : left) {
            logger.info("[KnK Region] " + player.getName() + " left region: " + region);
        }
    }
    
}

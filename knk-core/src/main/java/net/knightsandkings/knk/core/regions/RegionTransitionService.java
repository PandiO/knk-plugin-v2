package net.knightsandkings.knk.core.regions;

import java.util.Set;
import java.util.UUID;

/**
 * Core contract to decide whether a region transition is allowed and what message to show.
 * Remains Paper/WorldGuard agnostic.
 */
public interface RegionTransitionService {
    RegionTransitionDecision handleRegionTransition(
            UUID playerId,
            Set<String> oldRegionIds,
            Set<String> newRegionIds
    );
}

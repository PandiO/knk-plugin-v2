package net.knightsandkings.knk.core.eventHandlers;

import java.util.UUID;

public interface RegionEventHandler {
    void onRegionEnter(UUID uuid, String regionId);
    void onRegionLeave(UUID uuid, String regionId);
}

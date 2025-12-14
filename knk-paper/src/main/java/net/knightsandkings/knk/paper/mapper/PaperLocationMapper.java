package net.knightsandkings.knk.paper.mapper;

import net.knightsandkings.knk.core.domain.location.KnkLocation;
import org.bukkit.Location;

/**
 * Mapper between Bukkit Location and core KnkLocation.
 */
public final class PaperLocationMapper {
    private PaperLocationMapper() {}

    public static KnkLocation fromBukkit(Location loc) {
        if (loc == null) return null;
        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : null;
        return new KnkLocation(
                null, // id unknown here
                null, // name not defined in Bukkit Location
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch(),
                worldName
        );
    }
}

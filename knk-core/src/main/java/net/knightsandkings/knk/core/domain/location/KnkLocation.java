package net.knightsandkings.knk.core.domain.location;

/**
 * Domain value object for Location, aligned with API LocationDto.
 * Only confirmed fields from the API contract are included.
 */
public record KnkLocation(
        Integer id,
        String name,
        Double x,
        Double y,
        Double z,
        Float yaw,
        Float pitch,
        String world
) {}

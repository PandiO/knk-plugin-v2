package net.knightsandkings.knk.core.regions;

import java.util.Optional;

/**
 * Represents the decision for a WorldGuard region transition.
 */
public final class RegionTransitionDecision {
    private final boolean movementAllowed;
    private Optional<String> message;
    private final RegionTransitionType type;

    private RegionTransitionDecision(boolean movementAllowed, String message2, RegionTransitionType type) {
        this.movementAllowed = movementAllowed;
        this.message = message2 != null ? Optional.of(message2) : Optional.empty();
        this.type = type;
    }

    public static RegionTransitionDecision allow(RegionTransitionType type) {
        return new RegionTransitionDecision(true, null, type);
    }

    public static RegionTransitionDecision allow(RegionTransitionType type, String message) {
        return new RegionTransitionDecision(true, message, type);
    }

    public static RegionTransitionDecision deny(RegionTransitionType type, String message) {
        return new RegionTransitionDecision(false, message, type);
    }

    public boolean isMovementAllowed() {
        return movementAllowed;
    }

    public Optional<String> getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = Optional.ofNullable(message);
    }

    public RegionTransitionType getType() {
        return type;
    }
}

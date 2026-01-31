package net.knightsandkings.knk.core.domain.gates;

/**
 * Enum representing the current animation state of a gate.
 */
public enum AnimationState {
    /** Gate is fully closed and not animating */
    CLOSED,
    
    /** Gate is currently opening (animating from closed to open) */
    OPENING,
    
    /** Gate is fully open and not animating */
    OPEN,
    
    /** Gate is currently closing (animating from open to closed) */
    CLOSING
}

package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.domain.gates.CachedGate;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * Applies a directional push to entities based on gate orientation.
 */
public class EntityPusher {
    private static final double DEFAULT_PUSH_FORCE = 0.6;
    private static final double DIAGONAL_FACTOR = 0.70710678118;

    public static void pushEntity(Entity entity, CachedGate gate) {
        if (entity == null || gate == null) {
            return;
        }

        Vector direction = resolvePushDirection(gate);
        if (direction == null || direction.lengthSquared() == 0) {
            return;
        }

        direction.setY(0);
        if (direction.lengthSquared() == 0) {
            return;
        }

        Vector pushForce = direction.normalize().multiply(DEFAULT_PUSH_FORCE);
        entity.setVelocity(pushForce);
    }

    private static Vector resolvePushDirection(CachedGate gate) {
        Vector faceDirection = vectorFromFaceDirection(gate.getFaceDirection());
        if (faceDirection != null && faceDirection.lengthSquared() > 0) {
            return faceDirection.clone();
        }

        Vector nAxis = gate.getNAxis();
        if (nAxis != null && nAxis.lengthSquared() > 0) {
            return nAxis.clone();
        }

        Vector motionVector = gate.getMotionVector();
        if (motionVector != null && motionVector.lengthSquared() > 0) {
            return motionVector.clone();
        }

        return new Vector(0, 0, 0);
    }

    private static Vector vectorFromFaceDirection(String faceDirection) {
        if (faceDirection == null || faceDirection.isBlank()) {
            return null;
        }

        switch (faceDirection.trim().toLowerCase()) {
            case "north":
                return new Vector(0, 0, -1);
            case "north-east":
                return new Vector(DIAGONAL_FACTOR, 0, -DIAGONAL_FACTOR);
            case "east":
                return new Vector(1, 0, 0);
            case "south-east":
                return new Vector(DIAGONAL_FACTOR, 0, DIAGONAL_FACTOR);
            case "south":
                return new Vector(0, 0, 1);
            case "south-west":
                return new Vector(-DIAGONAL_FACTOR, 0, DIAGONAL_FACTOR);
            case "west":
                return new Vector(-1, 0, 0);
            case "north-west":
                return new Vector(-DIAGONAL_FACTOR, 0, -DIAGONAL_FACTOR);
            default:
                return null;
        }
    }
}

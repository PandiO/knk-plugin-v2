package net.knightsandkings.knk.core.domain.gates;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Cached representation of a gate structure with precomputed animation data.
 * This model is optimized for runtime animation calculations.
 */
public class CachedGate {
    // === Core Identity ===
    private final int id;
    private final String name;
    private final String gateType;
    private final String motionType;
    private final String geometryDefinitionMode;
    private final String faceDirection;

    // === Animation State (mutable) ===
    private AnimationState currentState;
    private int currentFrame;
    private long animationStartTime;

    // === Animation Configuration ===
    private final int animationDurationTicks;
    private final int animationTickRate;

    // === Geometry ===
    private final Vector anchorPoint;
    private final int geometryWidth;
    private final int geometryHeight;
    private final int geometryDepth;

    // === Precomputed Local Basis Vectors ===
    private Vector uAxis;  // Width direction
    private Vector vAxis;  // Height direction
    private Vector nAxis;  // Normal/motion direction

    // === Precomputed Motion ===
    private Vector motionVector;  // Direction and magnitude of motion
    private Vector hingeAxis;     // For rotation gates

    // === Block Data ===
    private final List<BlockSnapshot> blocks;

    // === Health & State ===
    private double healthCurrent;
    private double healthMax;
    private boolean isActive;
    private boolean isDestroyed;
    private boolean isInvincible;

    // === Rotation (for DRAWBRIDGE/DOUBLE_DOORS) ===
    private final int rotationMaxAngleDegrees;

    // === WorldGuard Integration ===
    private String regionClosedId;
    private String regionOpenedId;

    // === Respawn System ===
    private boolean canRespawn;
    private int respawnRateSeconds;
    private long respawnScheduledTime; // When respawn task is scheduled for

    public CachedGate(int id, String name, String gateType, String motionType, String geometryDefinitionMode,
                      int animationDurationTicks, int animationTickRate,
                      Vector anchorPoint, int geometryWidth, int geometryHeight, int geometryDepth,
                      double healthCurrent, double healthMax, boolean isActive, boolean isDestroyed, 
                      boolean isInvincible, int rotationMaxAngleDegrees, String faceDirection) {
        this.id = id;
        this.name = name;
        this.gateType = gateType;
        this.motionType = motionType;
        this.geometryDefinitionMode = geometryDefinitionMode;
        this.faceDirection = faceDirection;
        this.animationDurationTicks = animationDurationTicks;
        this.animationTickRate = animationTickRate;
        this.anchorPoint = anchorPoint;
        this.geometryWidth = geometryWidth;
        this.geometryHeight = geometryHeight;
        this.geometryDepth = geometryDepth;
        this.healthCurrent = healthCurrent;
        this.healthMax = healthMax;
        this.isActive = isActive;
        this.isDestroyed = isDestroyed;
        this.isInvincible = isInvincible;
        this.rotationMaxAngleDegrees = rotationMaxAngleDegrees;
        this.blocks = new ArrayList<>();
        this.currentState = AnimationState.CLOSED;
        this.currentFrame = 0;
        this.animationStartTime = 0;
        this.regionClosedId = "";
        this.regionOpenedId = "";
        this.canRespawn = true;
        this.respawnRateSeconds = 300;
        this.respawnScheduledTime = 0;
    }

    // === Getters ===

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGateType() {
        return gateType;
    }

    public String getMotionType() {
        return motionType;
    }

    public String getGeometryDefinitionMode() {
        return geometryDefinitionMode;
    }

    public String getFaceDirection() {
        return faceDirection;
    }

    public AnimationState getCurrentState() {
        return currentState;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public long getAnimationStartTime() {
        return animationStartTime;
    }

    public int getAnimationDurationTicks() {
        return animationDurationTicks;
    }

    public int getAnimationTickRate() {
        return animationTickRate;
    }

    public Vector getAnchorPoint() {
        return anchorPoint;
    }

    public int getGeometryWidth() {
        return geometryWidth;
    }

    public int getGeometryHeight() {
        return geometryHeight;
    }

    public int getGeometryDepth() {
        return geometryDepth;
    }

    public Vector getUAxis() {
        return uAxis;
    }

    public Vector getVAxis() {
        return vAxis;
    }

    public Vector getNAxis() {
        return nAxis;
    }

    public Vector getMotionVector() {
        return motionVector;
    }

    public Vector getHingeAxis() {
        return hingeAxis;
    }

    public List<BlockSnapshot> getBlocks() {
        return blocks;
    }

    public double getHealthCurrent() {
        return healthCurrent;
    }

    public double getHealthMax() {
        return healthMax;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public boolean isInvincible() {
        return isInvincible;
    }

    public int getRotationMaxAngleDegrees() {
        return rotationMaxAngleDegrees;
    }

    public String getRegionClosedId() {
        return regionClosedId;
    }

    public String getRegionOpenedId() {
        return regionOpenedId;
    }

    public boolean isCanRespawn() {
        return canRespawn;
    }

    public int getRespawnRateSeconds() {
        return respawnRateSeconds;
    }

    public long getRespawnScheduledTime() {
        return respawnScheduledTime;
    }

    // === Setters for Mutable State ===

    public void setCurrentState(AnimationState currentState) {
        this.currentState = currentState;
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void setAnimationStartTime(long animationStartTime) {
        this.animationStartTime = animationStartTime;
    }

    public void setHealthCurrent(double healthCurrent) {
        this.healthCurrent = healthCurrent;
    }

    public void setHealthMax(double healthMax) {
        this.healthMax = healthMax;
    }

    public void setIsDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setIsInvincible(boolean isInvincible) {
        this.isInvincible = isInvincible;
    }

    public void setRegionClosedId(String regionClosedId) {
        this.regionClosedId = regionClosedId != null ? regionClosedId : "";
    }

    public void setRegionOpenedId(String regionOpenedId) {
        this.regionOpenedId = regionOpenedId != null ? regionOpenedId : "";
    }

    public void setCanRespawn(boolean canRespawn) {
        this.canRespawn = canRespawn;
    }

    public void setRespawnRateSeconds(int respawnRateSeconds) {
        this.respawnRateSeconds = respawnRateSeconds;
    }

    public void setRespawnScheduledTime(long respawnScheduledTime) {
        this.respawnScheduledTime = respawnScheduledTime;
    }

    // === Setters for Precomputed Data ===

    public void setUAxis(Vector uAxis) {
        this.uAxis = uAxis;
    }

    public void setVAxis(Vector vAxis) {
        this.vAxis = vAxis;
    }

    public void setNAxis(Vector nAxis) {
        this.nAxis = nAxis;
    }

    public void setMotionVector(Vector motionVector) {
        this.motionVector = motionVector;
    }

    public void setHingeAxis(Vector hingeAxis) {
        this.hingeAxis = hingeAxis;
    }

    // === Helper Methods ===

    public void addBlock(BlockSnapshot block) {
        this.blocks.add(block);
    }

    public boolean isAnimating() {
        return currentState == AnimationState.OPENING || currentState == AnimationState.CLOSING;
    }
}

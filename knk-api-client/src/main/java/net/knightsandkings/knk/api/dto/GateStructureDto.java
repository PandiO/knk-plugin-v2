package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for gate structure details from Web API.
 * Maps to GateStructureReadDto from knk-web-api-v2.
 */
public class GateStructureDto {

    // === Core Identity ===
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("domainId")
    private Integer domainId;

    @JsonProperty("districtId")
    private Integer districtId;

    @JsonProperty("streetId")
    private Integer streetId;

    // === State ===
    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("isOpened")
    private Boolean isOpened;

    @JsonProperty("isDestroyed")
    private Boolean isDestroyed;

    @JsonProperty("isInvincible")
    private Boolean isInvincible;

    @JsonProperty("canRespawn")
    private Boolean canRespawn;

    @JsonProperty("healthCurrent")
    private Double healthCurrent;

    @JsonProperty("healthMax")
    private Double healthMax;

    @JsonProperty("respawnRateSeconds")
    private Integer respawnRateSeconds;

    // === Orientation ===
    @JsonProperty("faceDirection")
    private String faceDirection;

    // === Gate Type Configuration ===
    @JsonProperty("gateType")
    private String gateType;

    @JsonProperty("motionType")
    private String motionType;

    @JsonProperty("geometryDefinitionMode")
    private String geometryDefinitionMode;

    // === Animation Timing ===
    @JsonProperty("animationDurationTicks")
    private Integer animationDurationTicks;

    @JsonProperty("animationTickRate")
    private Integer animationTickRate;

    // === PLANE_GRID Geometry ===
    @JsonProperty("anchorPoint")
    private String anchorPoint;

    @JsonProperty("referencePoint1")
    private String referencePoint1;

    @JsonProperty("referencePoint2")
    private String referencePoint2;

    @JsonProperty("geometryWidth")
    private Integer geometryWidth;

    @JsonProperty("geometryHeight")
    private Integer geometryHeight;

    @JsonProperty("geometryDepth")
    private Integer geometryDepth;

    // === FLOOD_FILL Geometry ===
    @JsonProperty("seedBlocks")
    private String seedBlocks;

    @JsonProperty("scanMaxBlocks")
    private Integer scanMaxBlocks;

    @JsonProperty("scanMaxRadius")
    private Integer scanMaxRadius;

    @JsonProperty("scanMaterialWhitelist")
    private String scanMaterialWhitelist;

    @JsonProperty("scanMaterialBlacklist")
    private String scanMaterialBlacklist;

    @JsonProperty("scanPlaneConstraint")
    private Boolean scanPlaneConstraint;

    // === Block Rendering ===
    @JsonProperty("fallbackMaterialRefId")
    private Integer fallbackMaterialRefId;

    @JsonProperty("tileEntityPolicy")
    private String tileEntityPolicy;

    // === Rotation ===
    @JsonProperty("rotationMaxAngleDegrees")
    private Integer rotationMaxAngleDegrees;

    @JsonProperty("hingeAxis")
    private String hingeAxis;

    // === Double Doors ===
    @JsonProperty("leftDoorSeedBlock")
    private String leftDoorSeedBlock;

    @JsonProperty("rightDoorSeedBlock")
    private String rightDoorSeedBlock;

    @JsonProperty("mirrorRotation")
    private Boolean mirrorRotation;

    // === WorldGuard Integration ===
    @JsonProperty("regionClosedId")
    private String regionClosedId;

    @JsonProperty("regionOpenedId")
    private String regionOpenedId;

    // === Block Snapshots ===
    @JsonProperty("blockSnapshots")
    private List<GateBlockSnapshotDto> blockSnapshots;

    // === Getters and Setters ===

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public void setDomainId(Integer domainId) {
        this.domainId = domainId;
    }

    public Integer getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Integer districtId) {
        this.districtId = districtId;
    }

    public Integer getStreetId() {
        return streetId;
    }

    public void setStreetId(Integer streetId) {
        this.streetId = streetId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsOpened() {
        return isOpened;
    }

    public void setIsOpened(Boolean isOpened) {
        this.isOpened = isOpened;
    }

    public Boolean getIsDestroyed() {
        return isDestroyed;
    }

    public void setIsDestroyed(Boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

    public Boolean getIsInvincible() {
        return isInvincible;
    }

    public void setIsInvincible(Boolean isInvincible) {
        this.isInvincible = isInvincible;
    }

    public Boolean getCanRespawn() {
        return canRespawn;
    }

    public void setCanRespawn(Boolean canRespawn) {
        this.canRespawn = canRespawn;
    }

    public Double getHealthCurrent() {
        return healthCurrent;
    }

    public void setHealthCurrent(Double healthCurrent) {
        this.healthCurrent = healthCurrent;
    }

    public Double getHealthMax() {
        return healthMax;
    }

    public void setHealthMax(Double healthMax) {
        this.healthMax = healthMax;
    }

    public Integer getRespawnRateSeconds() {
        return respawnRateSeconds;
    }

    public void setRespawnRateSeconds(Integer respawnRateSeconds) {
        this.respawnRateSeconds = respawnRateSeconds;
    }

    public String getFaceDirection() {
        return faceDirection;
    }

    public void setFaceDirection(String faceDirection) {
        this.faceDirection = faceDirection;
    }

    public String getGateType() {
        return gateType;
    }

    public void setGateType(String gateType) {
        this.gateType = gateType;
    }

    public String getMotionType() {
        return motionType;
    }

    public void setMotionType(String motionType) {
        this.motionType = motionType;
    }

    public String getGeometryDefinitionMode() {
        return geometryDefinitionMode;
    }

    public void setGeometryDefinitionMode(String geometryDefinitionMode) {
        this.geometryDefinitionMode = geometryDefinitionMode;
    }

    public Integer getAnimationDurationTicks() {
        return animationDurationTicks;
    }

    public void setAnimationDurationTicks(Integer animationDurationTicks) {
        this.animationDurationTicks = animationDurationTicks;
    }

    public Integer getAnimationTickRate() {
        return animationTickRate;
    }

    public void setAnimationTickRate(Integer animationTickRate) {
        this.animationTickRate = animationTickRate;
    }

    public String getAnchorPoint() {
        return anchorPoint;
    }

    public void setAnchorPoint(String anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    public String getReferencePoint1() {
        return referencePoint1;
    }

    public void setReferencePoint1(String referencePoint1) {
        this.referencePoint1 = referencePoint1;
    }

    public String getReferencePoint2() {
        return referencePoint2;
    }

    public void setReferencePoint2(String referencePoint2) {
        this.referencePoint2 = referencePoint2;
    }

    public Integer getGeometryWidth() {
        return geometryWidth;
    }

    public void setGeometryWidth(Integer geometryWidth) {
        this.geometryWidth = geometryWidth;
    }

    public Integer getGeometryHeight() {
        return geometryHeight;
    }

    public void setGeometryHeight(Integer geometryHeight) {
        this.geometryHeight = geometryHeight;
    }

    public Integer getGeometryDepth() {
        return geometryDepth;
    }

    public void setGeometryDepth(Integer geometryDepth) {
        this.geometryDepth = geometryDepth;
    }

    public String getSeedBlocks() {
        return seedBlocks;
    }

    public void setSeedBlocks(String seedBlocks) {
        this.seedBlocks = seedBlocks;
    }

    public Integer getScanMaxBlocks() {
        return scanMaxBlocks;
    }

    public void setScanMaxBlocks(Integer scanMaxBlocks) {
        this.scanMaxBlocks = scanMaxBlocks;
    }

    public Integer getScanMaxRadius() {
        return scanMaxRadius;
    }

    public void setScanMaxRadius(Integer scanMaxRadius) {
        this.scanMaxRadius = scanMaxRadius;
    }

    public String getScanMaterialWhitelist() {
        return scanMaterialWhitelist;
    }

    public void setScanMaterialWhitelist(String scanMaterialWhitelist) {
        this.scanMaterialWhitelist = scanMaterialWhitelist;
    }

    public String getScanMaterialBlacklist() {
        return scanMaterialBlacklist;
    }

    public void setScanMaterialBlacklist(String scanMaterialBlacklist) {
        this.scanMaterialBlacklist = scanMaterialBlacklist;
    }

    public Boolean getScanPlaneConstraint() {
        return scanPlaneConstraint;
    }

    public void setScanPlaneConstraint(Boolean scanPlaneConstraint) {
        this.scanPlaneConstraint = scanPlaneConstraint;
    }

    public Integer getFallbackMaterialRefId() {
        return fallbackMaterialRefId;
    }

    public void setFallbackMaterialRefId(Integer fallbackMaterialRefId) {
        this.fallbackMaterialRefId = fallbackMaterialRefId;
    }

    public String getTileEntityPolicy() {
        return tileEntityPolicy;
    }

    public void setTileEntityPolicy(String tileEntityPolicy) {
        this.tileEntityPolicy = tileEntityPolicy;
    }

    public Integer getRotationMaxAngleDegrees() {
        return rotationMaxAngleDegrees;
    }

    public void setRotationMaxAngleDegrees(Integer rotationMaxAngleDegrees) {
        this.rotationMaxAngleDegrees = rotationMaxAngleDegrees;
    }

    public String getHingeAxis() {
        return hingeAxis;
    }

    public void setHingeAxis(String hingeAxis) {
        this.hingeAxis = hingeAxis;
    }

    public String getLeftDoorSeedBlock() {
        return leftDoorSeedBlock;
    }

    public void setLeftDoorSeedBlock(String leftDoorSeedBlock) {
        this.leftDoorSeedBlock = leftDoorSeedBlock;
    }

    public String getRightDoorSeedBlock() {
        return rightDoorSeedBlock;
    }

    public void setRightDoorSeedBlock(String rightDoorSeedBlock) {
        this.rightDoorSeedBlock = rightDoorSeedBlock;
    }

    public Boolean getMirrorRotation() {
        return mirrorRotation;
    }

    public void setMirrorRotation(Boolean mirrorRotation) {
        this.mirrorRotation = mirrorRotation;
    }

    public String getRegionClosedId() {
        return regionClosedId;
    }

    public void setRegionClosedId(String regionClosedId) {
        this.regionClosedId = regionClosedId;
    }

    public String getRegionOpenedId() {
        return regionOpenedId;
    }

    public void setRegionOpenedId(String regionOpenedId) {
        this.regionOpenedId = regionOpenedId;
    }

    public List<GateBlockSnapshotDto> getBlockSnapshots() {
        return blockSnapshots;
    }

    public void setBlockSnapshots(List<GateBlockSnapshotDto> blockSnapshots) {
        this.blockSnapshots = blockSnapshots;
    }
}

package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.api.dto.GateBlockSnapshotDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;
import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.BlockSnapshot;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import net.knightsandkings.knk.core.util.CoordinateParser;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Adapter for loading gates from API DTOs into the GateManager cache.
 * This class handles the conversion from API DTOs to domain objects.
 * 
 * Exists in knk-paper (not knk-core) to avoid circular dependency between
 * knk-core and knk-api-client. This follows hexagonal architecture:
 * - Core business logic lives in knk-core (GateManager state machine, etc.)
 * - Framework adapters (DTO conversions) live in knk-paper
 */
public class GateLoaderAdapter {
    private static final Logger LOGGER = Logger.getLogger(GateLoaderAdapter.class.getName());

    private final GateManager gateManager;

    public GateLoaderAdapter(GateManager gateManager) {
        this.gateManager = gateManager;
    }

    /**
     * Load and cache a single gate from a DTO.
     * This method handles all DTO-to-domain conversion.
     *
     * @param dto Gate structure DTO from API
     * @param snapshotDtos List of block snapshot DTOs
     */
    public void loadAndCacheGate(GateStructureDto dto, List<GateBlockSnapshotDto> snapshotDtos) {
        if (dto == null || dto.getId() == null) {
            LOGGER.warning("Cannot load gate: DTO or ID is null");
            return;
        }

        CachedGate cachedGate = buildCachedGate(dto, snapshotDtos);
        gateManager.cacheGate(cachedGate);
        
        LOGGER.info("Cached gate: " + cachedGate.getName() + " (ID: " + cachedGate.getId() + 
                   ") with " + cachedGate.getBlocks().size() + " blocks");
    }

    /**
     * Build a CachedGate from DTO data.
     * Precomputes local basis vectors and motion vectors.
     *
     * @param dto Gate structure DTO
     * @param snapshotDtos List of block snapshot DTOs
     * @return CachedGate instance
     */
    private CachedGate buildCachedGate(GateStructureDto dto, List<GateBlockSnapshotDto> snapshotDtos) {
        // Parse anchor point
        Vector anchorPoint = CoordinateParser.parseCoordinate(dto.getAnchorPoint());
        if (anchorPoint == null) {
            LOGGER.warning("Gate " + dto.getName() + " has invalid anchor point, using (0,0,0)");
            anchorPoint = new Vector(0, 0, 0);
        }

        // Create CachedGate
        CachedGate gate = new CachedGate(
            dto.getId(),
            dto.getName(),
            dto.getGateType() != null ? dto.getGateType() : "SLIDING",
            dto.getMotionType() != null ? dto.getMotionType() : "VERTICAL",
            dto.getGeometryDefinitionMode() != null ? dto.getGeometryDefinitionMode() : "PLANE_GRID",
            dto.getAnimationDurationTicks() != null ? dto.getAnimationDurationTicks() : 60,
            dto.getAnimationTickRate() != null ? dto.getAnimationTickRate() : 1,
            anchorPoint,
            dto.getGeometryWidth() != null ? dto.getGeometryWidth() : 0,
            dto.getGeometryHeight() != null ? dto.getGeometryHeight() : 0,
            dto.getGeometryDepth() != null ? dto.getGeometryDepth() : 0,
            dto.getHealthCurrent() != null ? dto.getHealthCurrent() : 500.0,
            dto.getHealthMax() != null ? dto.getHealthMax() : 500.0,
            dto.getIsActive() != null ? dto.getIsActive() : false,
            dto.getIsDestroyed() != null ? dto.getIsDestroyed() : false,
            dto.getIsInvincible() != null ? dto.getIsInvincible() : true,
            dto.getRotationMaxAngleDegrees() != null ? dto.getRotationMaxAngleDegrees() : 90
        );

        // Precompute local basis vectors
        precomputeBasisVectors(gate, dto);

        // Precompute motion vector
        precomputeMotionVector(gate);

        // Load block snapshots
        loadBlockSnapshots(gate, snapshotDtos);

        // Set initial state based on IsOpened
        if (dto.getIsOpened() != null && dto.getIsOpened()) {
            gate.setCurrentState(AnimationState.OPEN);
            gate.setCurrentFrame(gate.getAnimationDurationTicks());
        } else {
            gate.setCurrentState(AnimationState.CLOSED);
            gate.setCurrentFrame(0);
        }

        return gate;
    }

    /**
     * Precompute local basis vectors (u, v, n) from reference points.
     * For PLANE_GRID geometry mode.
     */
    private void precomputeBasisVectors(CachedGate gate, GateStructureDto dto) {
        Vector ref1 = CoordinateParser.parseCoordinate(dto.getReferencePoint1());
        Vector ref2 = CoordinateParser.parseCoordinate(dto.getReferencePoint2());
        Vector anchor = gate.getAnchorPoint();

        if (ref1 != null && ref2 != null && anchor != null) {
            // u-axis: direction from anchor to ref1 (width direction)
            Vector u = ref1.clone().subtract(anchor).normalize();
            gate.setUAxis(u);

            // v-axis: direction from anchor to ref2 (height direction)
            Vector v = ref2.clone().subtract(anchor).normalize();
            gate.setVAxis(v);

            // n-axis: cross product (normal direction, motion axis)
            Vector n = u.clone().crossProduct(v).normalize();
            gate.setNAxis(n);

            LOGGER.fine("Gate " + gate.getName() + " basis vectors: u=" + u + ", v=" + v + ", n=" + n);
        } else {
            // Fallback to standard axes
            gate.setUAxis(new Vector(1, 0, 0));
            gate.setVAxis(new Vector(0, 1, 0));
            gate.setNAxis(new Vector(0, 0, 1));
            LOGGER.warning("Gate " + gate.getName() + " missing reference points, using default axes");
        }
    }

    /**
     * Precompute motion vector based on motion type and geometry.
     */
    private void precomputeMotionVector(CachedGate gate) {
        String motionType = gate.getMotionType();
        Vector nAxis = gate.getNAxis();

        if (motionType == null || nAxis == null) {
            gate.setMotionVector(new Vector(0, 0, 0));
            return;
        }

        switch (motionType) {
            case "VERTICAL":
                // Move upward along v-axis (or just +Y)
                gate.setMotionVector(new Vector(0, gate.getGeometryDepth(), 0));
                break;
            case "LATERAL":
                // Move along n-axis
                gate.setMotionVector(nAxis.clone().multiply(gate.getGeometryDepth()));
                break;
            case "ROTATION":
                // No linear motion vector, rotation handled separately
                gate.setMotionVector(new Vector(0, 0, 0));
                // Set hinge axis from DTO if available
                gate.setHingeAxis(nAxis);
                break;
            default:
                gate.setMotionVector(new Vector(0, 0, 0));
        }

        LOGGER.fine("Gate " + gate.getName() + " motion vector: " + gate.getMotionVector());
    }

    /**
     * Load block snapshots into the gate.
     * Sorts by SortOrder to ensure stable block placement order.
     */
    private void loadBlockSnapshots(CachedGate gate, List<GateBlockSnapshotDto> snapshotDtos) {
        // Sort by SortOrder
        snapshotDtos.sort(Comparator.comparingInt(GateBlockSnapshotDto::sortOrder));

        for (GateBlockSnapshotDto dto : snapshotDtos) {
            Vector relativePos = new Vector(
                dto.relativeX() != null ? dto.relativeX() : 0,
                dto.relativeY() != null ? dto.relativeY() : 0,
                dto.relativeZ() != null ? dto.relativeZ() : 0
            );

            BlockSnapshot snapshot = new BlockSnapshot(
                dto.id(),
                relativePos,
                dto.minecraftBlockRefId() != null ? dto.minecraftBlockRefId() : 0,
                dto.blockData() != null ? dto.blockData() : "",
                dto.sortOrder() != null ? dto.sortOrder() : 0
            );

            gate.addBlock(snapshot);
        }

        LOGGER.fine("Loaded " + gate.getBlocks().size() + " blocks for gate " + gate.getName());
    }
}

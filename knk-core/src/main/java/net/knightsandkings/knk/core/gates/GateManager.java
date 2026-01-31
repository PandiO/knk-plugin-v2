package net.knightsandkings.knk.core.gates;

import net.knightsandkings.knk.api.dto.GateBlockSnapshotDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;
import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.BlockSnapshot;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.ports.api.GateStructuresApi;
import net.knightsandkings.knk.core.util.CoordinateParser;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Manager for gate structures in the plugin.
 * Loads gates from the API, caches them in memory, and provides access to gate data.
 * This class handles the initial loading and caching but does NOT handle animation logic.
 */
public class GateManager {
    private static final Logger LOGGER = Logger.getLogger(GateManager.class.getName());

    private final GateStructuresApi gateApi;
    private final Map<Integer, CachedGate> gateCache;

    public GateManager(GateStructuresApi gateApi) {
        this.gateApi = gateApi;
        this.gateCache = new HashMap<>();
    }

    /**
     * Load all active gate structures from the API.
     * This method should be called during plugin startup.
     *
     * @return CompletableFuture that completes when gates are loaded
     */
    public CompletableFuture<Void> loadGatesFromApi() {
        return gateApi.getAll()
            .thenCompose(gates -> {
                LOGGER.info("Fetched " + gates.size() + " gates from API");
                
                // Load each gate's snapshots and cache
                CompletableFuture<?>[] futures = gates.stream()
                    .filter(dto -> dto.getIsActive() != null && dto.getIsActive())
                    .map(this::loadAndCacheGate)
                    .toArray(CompletableFuture[]::new);
                
                return CompletableFuture.allOf(futures);
            })
            .thenRun(() -> {
                LOGGER.info("Successfully loaded " + gateCache.size() + " active gates into cache");
            })
            .exceptionally(ex -> {
                LOGGER.severe("Failed to load gates from API: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
    }

    /**
     * Load a single gate and its snapshots, then cache it.
     *
     * @param dto Gate structure DTO from API
     * @return CompletableFuture that completes when gate is cached
     */
    private CompletableFuture<Void> loadAndCacheGate(GateStructureDto dto) {
        return gateApi.getGateSnapshots(dto.getId())
            .thenAccept(snapshots -> {
                CachedGate cachedGate = buildCachedGate(dto, snapshots);
                gateCache.put(cachedGate.getId(), cachedGate);
                LOGGER.info("Cached gate: " + cachedGate.getName() + " (ID: " + cachedGate.getId() + 
                           ") with " + cachedGate.getBlocks().size() + " blocks");
            })
            .exceptionally(ex -> {
                LOGGER.warning("Failed to load snapshots for gate " + dto.getName() + 
                              " (ID: " + dto.getId() + "): " + ex.getMessage());
                return null;
            });
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
                GateStructureDto dto = null; // We don't have DTO here, set in buildCachedGate if needed
                // For now, use n-axis as hinge
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

    // === Public API for accessing gates ===

    /**
     * Get a cached gate by ID.
     *
     * @param id Gate ID
     * @return CachedGate or null if not found
     */
    public CachedGate getGate(int id) {
        return gateCache.get(id);
    }

    /**
     * Get a cached gate by name.
     *
     * @param name Gate name
     * @return CachedGate or null if not found
     */
    public CachedGate getGateByName(String name) {
        return gateCache.values().stream()
            .filter(gate -> gate.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get all cached gates.
     *
     * @return Map of gate ID to CachedGate
     */
    public Map<Integer, CachedGate> getAllGates() {
        return new HashMap<>(gateCache);
    }

    /**
     * Reload gates from the API.
     * Clears the cache and reloads all gates.
     *
     * @return CompletableFuture that completes when reload is done
     */
    public CompletableFuture<Void> reloadGates() {
        gateCache.clear();
        LOGGER.info("Cleared gate cache, reloading from API...");
        return loadGatesFromApi();
    }

    /**
     * Update gate state in the API.
     * This should be called when a gate opens or closes.
     *
     * @param gateId Gate ID
     * @param isOpened New opened state
     * @return CompletableFuture that completes when update is done
     */
    public CompletableFuture<Void> updateGateState(int gateId, boolean isOpened) {
        return gateApi.updateGateState(gateId, isOpened)
            .exceptionally(ex -> {
                LOGGER.warning("Failed to update gate state for ID " + gateId + ": " + ex.getMessage());
                return null;
            });
    }
}

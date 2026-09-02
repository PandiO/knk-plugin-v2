package net.knightsandkings.knk.paper.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.knightsandkings.knk.api.GateStructuresApi;
import net.knightsandkings.knk.api.dto.GateBlockSnapshotScanDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;
import net.knightsandkings.knk.api.dto.WorldTaskDto;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import net.knightsandkings.knk.core.util.CoordinateParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Headless WorldTask handler that scans a gate's blocks into GateBlockSnapshot rows.
 * Only PLANE_GRID geometry is implemented; FLOOD_FILL fails with a clear message (see class notes).
 *
 * Known limitations of this first implementation:
 * - FLOOD_FILL geometry mode is not implemented yet (fails the task with an explanatory message).
 * - Tile entity contents (chest inventory, sign text, etc.) are not captured; only a warning is emitted.
 */
public class GateBlockScanTaskHandler implements IHeadlessWorldTaskHandler {
    private static final Logger LOGGER = Logger.getLogger(GateBlockScanTaskHandler.class.getName());
    private static final String TASK_TYPE = "GateBlockScan";
    private static final int BLOCKS_PER_TICK = 200;
    private static final int BLOCKS_PER_TICK_WHEN_LAGGING = 50;
    private static final double LAG_TPS_THRESHOLD = 15.0;
    private static final int DEFAULT_SCAN_MAX_BLOCKS = 500; // matches GateStructure.ScanMaxBlocks default
    private static final int ABSOLUTE_MAX_CELLS = 20000; // hard ceiling regardless of gate configuration

    private final GateStructuresApi gateStructuresApi;
    private final WorldTasksApi worldTasksApi;
    private final Plugin plugin;

    public GateBlockScanTaskHandler(GateStructuresApi gateStructuresApi, WorldTasksApi worldTasksApi, Plugin plugin) {
        this.gateStructuresApi = gateStructuresApi;
        this.worldTasksApi = worldTasksApi;
        this.plugin = plugin;
    }

    @Override
    public boolean supports(String taskType) {
        return TASK_TYPE.equals(taskType);
    }

    @Override
    public void execute(WorldTaskDto task, Runnable onFinished) {
        Integer gateStructureId = parseGateStructureId(task.inputJson());
        if (gateStructureId == null) {
            fail(task.id(), "InputJson did not contain a valid gateStructureId.", onFinished);
            return;
        }

        gateStructuresApi.getById(gateStructureId).whenComplete((gate, error) -> {
            if (error != null || gate == null) {
                String reason = error != null ? error.getMessage() : "gate not found";
                fail(task.id(), "Could not load gate " + gateStructureId + ": " + reason, onFinished);
                return;
            }

            // Bukkit world/block access must happen on the main thread.
            plugin.getServer().getScheduler().runTask(plugin, () -> startScan(task.id(), gate, onFinished));
        });
    }

    private void startScan(int taskId, GateStructureDto gate, Runnable onFinished) {
        if (!"PLANE_GRID".equals(gate.getGeometryDefinitionMode())) {
            fail(taskId, "FLOOD_FILL scanning is not implemented yet for gate '" + gate.getName()
                + "'. Configure PLANE_GRID geometry or scan manually.", onFinished);
            return;
        }

        List<ScanWing> wings = buildScanWings(gate);
        if (wings.isEmpty()) {
            fail(taskId, "Gate '" + gate.getName() + "' is missing anchor/reference points required for scanning.", onFinished);
            return;
        }

        long totalCells = 0;
        for (ScanWing wing : wings) {
            totalCells += (long) wing.width * wing.height * wing.depth;
        }

        String sizeError = checkScanSizeLimit(totalCells, gate.getScanMaxBlocks());
        if (sizeError != null) {
            fail(taskId, "Scan area for gate '" + gate.getName() + "' " + sizeError, onFinished);
            return;
        }

        List<int[]> cells = new ArrayList<>();
        for (int wingIndex = 0; wingIndex < wings.size(); wingIndex++) {
            ScanWing wing = wings.get(wingIndex);
            for (int i = 0; i < wing.width; i++) {
                for (int j = 0; j < wing.height; j++) {
                    for (int k = 0; k < wing.depth; k++) {
                        cells.add(new int[]{wingIndex, i, j, k});
                    }
                }
            }
        }

        boolean captureTileEntities = !"NONE".equals(gate.getTileEntityPolicy());
        new ChunkedScanRunnable(taskId, gate, wings, cells, captureTileEntities, onFinished)
            .runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * One PLANE_GRID box per wing. Most gates have a single wing; DOUBLE_DOORS scans
     * both LeftDoorSeedBlock and RightDoorSeedBlock as independent boxes with the same footprint.
     */
    private List<ScanWing> buildScanWings(GateStructureDto gate) {
        List<ScanWing> wings = new ArrayList<>();

        Vector anchor = CoordinateParser.parseCoordinate(gate.getAnchorPoint());
        Vector ref1 = CoordinateParser.parseCoordinate(gate.getReferencePoint1());
        Vector ref2 = CoordinateParser.parseCoordinate(gate.getReferencePoint2());
        String worldName = CoordinateParser.parseWorldName(gate.getAnchorPoint());

        if (anchor == null || ref1 == null || ref2 == null || worldName.isBlank()) {
            return wings;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return wings;
        }

        Vector uAxis = ref1.clone().subtract(anchor).normalize();
        Vector vAxis = ref2.clone().subtract(anchor).normalize();
        Vector nAxis = uAxis.clone().crossProduct(vAxis).normalize();

        int width = Math.max(1, gate.getGeometryWidth() != null ? gate.getGeometryWidth() : 1);
        int height = Math.max(1, gate.getGeometryHeight() != null ? gate.getGeometryHeight() : 1);
        int depth = Math.max(1, gate.getGeometryDepth() != null ? gate.getGeometryDepth() : 1);

        String leftSeedJson = gate.getLeftDoorSeedBlock();
        String rightSeedJson = gate.getRightDoorSeedBlock();
        Vector leftSeed = CoordinateParser.parseCoordinate(leftSeedJson);
        Vector rightSeed = CoordinateParser.parseCoordinate(rightSeedJson);

        if ("DOUBLE_DOORS".equals(gate.getGateType()) && leftSeed != null && rightSeed != null) {
            wings.add(new ScanWing(world, leftSeed, uAxis, vAxis, nAxis, width, height, depth));
            wings.add(new ScanWing(world, rightSeed, uAxis, vAxis, nAxis, width, height, depth));
        } else {
            wings.add(new ScanWing(world, anchor, uAxis, vAxis, nAxis, width, height, depth));
        }

        return wings;
    }

    private void fail(int taskId, String message, Runnable onFinished) {
        LOGGER.warning("[GateBlockScan] Task " + taskId + " failed: " + message);
        worldTasksApi.fail(taskId, message).whenComplete((result, ex) -> onFinished.run());
    }

    private void complete(int taskId, String outputJson, Runnable onFinished) {
        worldTasksApi.complete(taskId, outputJson).whenComplete((result, ex) -> {
            if (ex != null) {
                LOGGER.warning("[GateBlockScan] Task " + taskId + " scan finished but complete() call failed: " + ex.getMessage());
            }
            onFinished.run();
        });
    }

    private Integer parseGateStructureId(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.parseString(inputJson).getAsJsonObject();
            if (obj.has("gateStructureId") && !obj.get("gateStructureId").isJsonNull()) {
                return obj.get("gateStructureId").getAsInt();
            }
        } catch (Exception e) {
            LOGGER.warning("[GateBlockScan] Could not parse InputJson: " + e.getMessage());
        }
        return null;
    }

    /**
     * Pure helper (no Bukkit dependency) so the size cap can be unit tested directly.
     * @return an error message fragment if the scan is too large, or null if it's within limits
     */
    static String checkScanSizeLimit(long totalCells, Integer configuredScanMaxBlocks) {
        int configuredCap = configuredScanMaxBlocks != null && configuredScanMaxBlocks > 0
            ? configuredScanMaxBlocks : DEFAULT_SCAN_MAX_BLOCKS;
        int effectiveCap = Math.min(configuredCap, ABSOLUTE_MAX_CELLS);

        if (totalCells > effectiveCap) {
            return "is too large: " + totalCells + " blocks exceeds the limit of " + effectiveCap
                + ". Reduce GeometryWidth/Height/Depth or increase ScanMaxBlocks.";
        }
        return null;
    }

    private record ScanWing(World world, Vector anchor, Vector uAxis, Vector vAxis, Vector nAxis,
                            int width, int height, int depth) {
    }

    /**
     * Scans a bounded number of cells per tick to avoid blocking the server thread
     * on large gates, and backs off further when the server is already lagging.
     */
    private class ChunkedScanRunnable extends BukkitRunnable {
        private final int taskId;
        private final GateStructureDto gate;
        private final List<ScanWing> wings;
        private final List<int[]> cells;
        private final boolean captureTileEntities;
        private final Runnable onFinished;

        private final List<GateBlockSnapshotScanDto> snapshots = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private int cursor = 0;
        private int sortOrder = 0;
        private int skippedUnloadedChunks = 0;
        private boolean tileEntityWarningAdded = false;

        ChunkedScanRunnable(int taskId, GateStructureDto gate, List<ScanWing> wings, List<int[]> cells,
                            boolean captureTileEntities, Runnable onFinished) {
            this.taskId = taskId;
            this.gate = gate;
            this.wings = wings;
            this.cells = cells;
            this.captureTileEntities = captureTileEntities;
            this.onFinished = onFinished;
        }

        @Override
        public void run() {
            int budget = isServerLagging() ? BLOCKS_PER_TICK_WHEN_LAGGING : BLOCKS_PER_TICK;
            int processed = 0;

            while (cursor < cells.size() && processed < budget) {
                processCell(cells.get(cursor));
                cursor++;
                processed++;
            }

            if (cursor >= cells.size()) {
                finish();
                cancel();
            }
        }

        private void processCell(int[] cell) {
            ScanWing wing = wings.get(cell[0]);
            int i = cell[1];
            int j = cell[2];
            int k = cell[3];

            Vector offset = wing.uAxis.clone().multiply(i)
                .add(wing.vAxis.clone().multiply(j))
                .add(wing.nAxis.clone().multiply(k));

            int relativeX = (int) Math.round(offset.getX());
            int relativeY = (int) Math.round(offset.getY());
            int relativeZ = (int) Math.round(offset.getZ());

            Vector worldPos = wing.anchor.clone().add(offset);
            int worldX = worldPos.getBlockX();
            int worldY = worldPos.getBlockY();
            int worldZ = worldPos.getBlockZ();

            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;
            if (!wing.world.isChunkLoaded(chunkX, chunkZ)) {
                skippedUnloadedChunks++;
                return;
            }

            Block block = wing.world.getBlockAt(worldX, worldY, worldZ);
            Material material = block.getType();

            String tileEntityJson = "{}";
            if (captureTileEntities && block.getState() instanceof TileState && !tileEntityWarningAdded) {
                warnings.add("Tile entity contents (inventory/text/etc.) are not captured yet; only the block type was recorded.");
                tileEntityWarningAdded = true;
            }

            snapshots.add(new GateBlockSnapshotScanDto(
                relativeX, relativeY, relativeZ,
                worldX, worldY, worldZ,
                material.getKey().toString(),
                block.getBlockData().getAsString(),
                tileEntityJson,
                sortOrder++
            ));
        }

        private boolean isServerLagging() {
            try {
                return Bukkit.getTPS()[0] < LAG_TPS_THRESHOLD;
            } catch (Exception e) {
                return false;
            }
        }

        private void finish() {
            if (skippedUnloadedChunks > 0) {
                warnings.add(skippedUnloadedChunks + " cell(s) were skipped because their chunk was not loaded.");
            }

            String status = warnings.isEmpty() ? "Success" : "Warning";
            if (snapshots.isEmpty()) {
                status = "Failed";
                warnings.add("No blocks were captured for gate '" + gate.getName() + "'.");
            }

            String outputJson = buildOutputJson(status, snapshots, warnings, null);

            if ("Failed".equals(status)) {
                fail(taskId, String.join(" ", warnings), onFinished);
            } else {
                complete(taskId, outputJson, onFinished);
            }
        }
    }

    private String buildOutputJson(String status, List<GateBlockSnapshotScanDto> snapshots, List<String> warnings, String errorMessage) {
        JsonObject root = new JsonObject();
        root.addProperty("status", status);
        root.addProperty("blockCount", snapshots.size());

        JsonArray snapshotArray = new JsonArray();
        for (GateBlockSnapshotScanDto snapshot : snapshots) {
            JsonObject obj = new JsonObject();
            obj.addProperty("relativeX", snapshot.relativeX());
            obj.addProperty("relativeY", snapshot.relativeY());
            obj.addProperty("relativeZ", snapshot.relativeZ());
            obj.addProperty("worldX", snapshot.worldX());
            obj.addProperty("worldY", snapshot.worldY());
            obj.addProperty("worldZ", snapshot.worldZ());
            obj.addProperty("materialName", snapshot.materialName());
            obj.addProperty("blockDataJson", snapshot.blockDataJson());
            obj.addProperty("tileEntityJson", snapshot.tileEntityJson());
            obj.addProperty("sortOrder", snapshot.sortOrder());
            snapshotArray.add(obj);
        }
        root.add("snapshots", snapshotArray);

        JsonArray warningsArray = new JsonArray();
        for (String warning : warnings) {
            warningsArray.add(warning);
        }
        root.add("warnings", warningsArray);

        if (errorMessage != null) {
            root.addProperty("errorMessage", errorMessage);
        }

        return root.toString();
    }
}

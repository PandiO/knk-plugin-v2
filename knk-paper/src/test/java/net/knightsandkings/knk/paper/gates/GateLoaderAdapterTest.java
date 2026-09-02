package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.api.dto.GateBlockSnapshotDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;
import net.knightsandkings.knk.api.GateStructuresApi;
import net.knightsandkings.knk.core.domain.gates.BlockSnapshot;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GateLoaderAdapterTest {
    private static final double EPSILON = 0.001;

    @Test
    void loadAndCacheGate_ComputesBasisVectorsAndMotion() {
        GateManager gateManager = new GateManager();
        GateLoaderAdapter adapter = new GateLoaderAdapter(gateManager);

        GateStructureDto dto = new GateStructureDto();
        dto.setId(1);
        dto.setName("Basis Gate");
        dto.setGateType("SLIDING");
        dto.setMotionType("LATERAL");
        dto.setGeometryDefinitionMode("PLANE_GRID");
        dto.setAnimationDurationTicks(60);
        dto.setAnimationTickRate(1);
        dto.setGeometryDepth(3);
        dto.setAnchorPoint("{\"x\":0,\"y\":0,\"z\":0}");
        dto.setReferencePoint1("{\"x\":1,\"y\":0,\"z\":0}");
        dto.setReferencePoint2("{\"x\":0,\"y\":1,\"z\":0}");

        List<GateBlockSnapshotDto> snapshots = new ArrayList<>();
        adapter.loadAndCacheGate(dto, snapshots);

        CachedGate gate = gateManager.getGate(1);
        assertNotNull(gate);

        Vector uAxis = gate.getUAxis();
        Vector vAxis = gate.getVAxis();
        Vector nAxis = gate.getNAxis();
        Vector motion = gate.getMotionVector();

        assertNotNull(uAxis);
        assertNotNull(vAxis);
        assertNotNull(nAxis);
        assertNotNull(motion);

        assertEquals(1, uAxis.getX(), EPSILON);
        assertEquals(1, vAxis.getY(), EPSILON);
        assertEquals(1, nAxis.getZ(), EPSILON);
        assertEquals(3, motion.getZ(), EPSILON);
    }

    @Test
    void loadAndCacheGate_UsesBlockDataJsonFromRealApiContract() {
        GateManager gateManager = new GateManager();
        GateLoaderAdapter adapter = new GateLoaderAdapter(gateManager);

        GateStructureDto dto = new GateStructureDto();
        dto.setId(11);
        dto.setName("Keep Gate test");
        dto.setGateType("SLIDING");
        dto.setMotionType("VERTICAL");
        dto.setGeometryDefinitionMode("PLANE_GRID");
        dto.setAnimationDurationTicks(60);
        dto.setAnimationTickRate(1);
        dto.setAnchorPoint("{\"x\":0,\"y\":0,\"z\":0}");

        GateBlockSnapshotDto snapshotWithBlockData = new GateBlockSnapshotDto(
            1, 11, 0, 0, 0, 0, 0, 0, "minecraft:oak_planks", "minecraft:oak_planks", "{}", 0
        );
        GateBlockSnapshotDto snapshotWithoutBlockData = new GateBlockSnapshotDto(
            2, 11, 1, 0, 0, 1, 0, 0, "minecraft:water", "", "{}", 1
        );

        adapter.loadAndCacheGate(dto, List.of(snapshotWithBlockData, snapshotWithoutBlockData));

        List<BlockSnapshot> blocks = gateManager.getGate(11).getBlocks();
        assertEquals(2, blocks.size());
        assertEquals("minecraft:oak_planks", blocks.get(0).getBlockData());
        assertEquals("minecraft:water", blocks.get(1).getBlockData());
    }

    @Test
    void loadAll_CachesEveryGateReturnedByTheApi() {
        GateManager gateManager = new GateManager();
        GateLoaderAdapter adapter = new GateLoaderAdapter(gateManager);
        GateStructuresApi api = mock(GateStructuresApi.class);

        GateStructureDto gateTen = createGate(10, "East Gate");
        GateStructureDto gateEleven = createGate(11, "West Gate");
        when(api.getAll()).thenReturn(CompletableFuture.completedFuture(List.of(gateTen, gateEleven)));
        when(api.getGateSnapshots(10)).thenReturn(CompletableFuture.completedFuture(List.of()));
        when(api.getGateSnapshots(11)).thenReturn(CompletableFuture.completedFuture(List.of()));

        adapter.loadAll(api).join();

        assertEquals(2, gateManager.getAllGates().size());
        assertEquals("East Gate", gateManager.getGate(10).getName());
        assertEquals("West Gate", gateManager.getGate(11).getName());
    }

    private GateStructureDto createGate(int id, String name) {
        GateStructureDto dto = new GateStructureDto();
        dto.setId(id);
        dto.setName(name);
        dto.setAnchorPoint("{\"x\":0,\"y\":64,\"z\":0}");
        return dto;
    }
}

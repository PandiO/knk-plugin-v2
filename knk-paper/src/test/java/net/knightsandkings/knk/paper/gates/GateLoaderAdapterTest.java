package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.api.dto.GateBlockSnapshotDto;
import net.knightsandkings.knk.api.dto.GateStructureDto;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
}

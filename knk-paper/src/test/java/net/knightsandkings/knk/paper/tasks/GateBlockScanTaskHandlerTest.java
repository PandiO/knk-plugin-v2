package net.knightsandkings.knk.paper.tasks;

import net.knightsandkings.knk.api.GateStructuresApi;
import net.knightsandkings.knk.api.dto.WorldTaskDto;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests covering the fallback/error paths of GateBlockScanTaskHandler that don't require
 * a live Bukkit World (see repo testing notes: full scans need Bukkit runtime).
 */
class GateBlockScanTaskHandlerTest {
    private GateStructuresApi mockGateStructuresApi;
    private WorldTasksApi mockWorldTasksApi;
    private Plugin mockPlugin;
    private GateBlockScanTaskHandler handler;

    @BeforeEach
    void setUp() {
        mockGateStructuresApi = mock(GateStructuresApi.class);
        mockWorldTasksApi = mock(WorldTasksApi.class);
        mockPlugin = mock(Plugin.class);
        handler = new GateBlockScanTaskHandler(mockGateStructuresApi, mockWorldTasksApi, mockPlugin);

        when(mockWorldTasksApi.fail(anyInt(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testCheckScanSizeLimit_WithinConfiguredCap_ReturnsNull() {
        assertNull(GateBlockScanTaskHandler.checkScanSizeLimit(400, 500));
    }

    @Test
    void testCheckScanSizeLimit_ExceedsConfiguredCap_ReturnsMessage() {
        String result = GateBlockScanTaskHandler.checkScanSizeLimit(600, 500);
        assertNotNull(result);
        assertTrue(result.contains("600"));
        assertTrue(result.contains("500"));
    }

    @Test
    void testCheckScanSizeLimit_NullConfiguredCap_FallsBackToDefault() {
        assertNull(GateBlockScanTaskHandler.checkScanSizeLimit(500, null));
        assertNotNull(GateBlockScanTaskHandler.checkScanSizeLimit(501, null));
    }

    @Test
    void testCheckScanSizeLimit_ConfiguredCapAboveAbsoluteMax_IsClampedToAbsoluteMax() {
        assertNotNull(GateBlockScanTaskHandler.checkScanSizeLimit(20001, 999999));
        assertNull(GateBlockScanTaskHandler.checkScanSizeLimit(20000, 999999));
    }

    @Test
    void testSupports_OnlyGateBlockScan() {
        assertTrue(handler.supports("GateBlockScan"));
        assertFalse(handler.supports("Location"));
        assertFalse(handler.supports(null));
    }

    @Test
    void testExecute_MissingGateStructureId_FailsTaskWithoutTouchingGateApi() {
        WorldTaskDto task = createTask(1, "{}");
        AtomicBoolean finished = new AtomicBoolean(false);

        handler.execute(task, () -> finished.set(true));

        verify(mockWorldTasksApi).fail(eq(1), contains("gateStructureId"));
        verify(mockGateStructuresApi, never()).getById(anyInt());
        assertTrue(finished.get());
    }

    @Test
    void testExecute_GateApiLookupFails_FailsTaskWithReason() {
        WorldTaskDto task = createTask(2, "{\"gateStructureId\":11}");
        when(mockGateStructuresApi.getById(11)).thenReturn(
            CompletableFuture.failedFuture(new ApiException("url", 500, "boom", "detail"))
        );
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<String> failMessage = new AtomicReference<>();
        when(mockWorldTasksApi.fail(eq(2), anyString())).thenAnswer(invocation -> {
            failMessage.set(invocation.getArgument(1));
            return CompletableFuture.completedFuture(null);
        });

        handler.execute(task, () -> finished.set(true));

        verify(mockWorldTasksApi, timeout(1000)).fail(eq(2), anyString());
        assertTrue(finished.get());
        assertNotNull(failMessage.get());
        assertTrue(failMessage.get().contains("11"));
    }

    @Test
    void testExecute_GateNotFound_FailsTask() {
        WorldTaskDto task = createTask(3, "{\"gateStructureId\":99}");
        when(mockGateStructuresApi.getById(99)).thenReturn(CompletableFuture.completedFuture(null));
        AtomicBoolean finished = new AtomicBoolean(false);

        handler.execute(task, () -> finished.set(true));

        verify(mockWorldTasksApi, timeout(1000)).fail(eq(3), anyString());
        assertTrue(finished.get());
    }

    private WorldTaskDto createTask(int id, String inputJson) {
        return new WorldTaskDto(
            id, 1, "GateStructure", 1, "BlockSnapshots", "GateBlockScan", "Pending",
            null, null, null, null, null,
            inputJson, null, null, null
        );
    }
}

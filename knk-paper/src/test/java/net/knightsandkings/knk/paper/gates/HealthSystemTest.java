package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.api.GateStructuresApi;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthSystem.
 * Tests health damage, gate destruction, and respawn mechanics.
 * 
 * NOTE: Some tests require Bukkit runtime and are disabled in unit test environment.
 */
public class HealthSystemTest {

    @Mock
    private GateManager mockGateManager;

    @Mock
    private GateStructuresApi mockGateStructuresApi;

    @Mock
    private org.bukkit.plugin.java.JavaPlugin mockPlugin;

    private HealthSystem healthSystem;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        healthSystem = new HealthSystem(mockGateManager, mockGateStructuresApi, mockPlugin);
    }

    @Test
    @Disabled("Requires Bukkit runtime for BukkitRunnable")
    public void testApplyDamageToVulnerableGate() {
        CachedGate gate = createTestGate();
        gate.setIsInvincible(false);
        gate.setHealthCurrent(100.0);

        healthSystem.applyDamage(gate, 25.0);

        assertEquals(75.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    @Disabled("Requires Bukkit runtime for BukkitRunnable")
    public void testApplyDamageToInvincibleGate() {
        CachedGate gate = createTestGate();
        gate.setIsInvincible(true);
        gate.setHealthCurrent(100.0);

        healthSystem.applyDamage(gate, 50.0);

        // Invincible gate health should not change
        assertEquals(100.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    @Disabled("Requires Bukkit runtime for BukkitRunnable")
    public void testApplyDamageMinimumZeroHealth() {
        CachedGate gate = createTestGate();
        gate.setIsInvincible(false);
        gate.setHealthCurrent(10.0);

        healthSystem.applyDamage(gate, 50.0);

        // Health should not go below 0
        assertEquals(0.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    public void testApplyNegativeDamageIsIgnored() {
        CachedGate gate = createTestGate();
        gate.setHealthCurrent(50.0);

        healthSystem.applyDamage(gate, -10.0);

        assertEquals(50.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    public void testApplyZeroDamageIsIgnored() {
        CachedGate gate = createTestGate();
        gate.setHealthCurrent(50.0);

        healthSystem.applyDamage(gate, 0.0);

        assertEquals(50.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    @Disabled("Requires Bukkit runtime for BukkitRunnable")
    public void testDestroyGateUpdatesState() {
        CachedGate gate = createTestGate();
        gate.setHealthCurrent(100.0);
        gate.setIsActive(true);
        gate.setIsDestroyed(false);

        // Call destroy without mocking Bukkit.getWorlds() to avoid NPE
        // The destroyGate method will just update the state without actually removing blocks
        healthSystem.destroyGate(gate);

        assertTrue(gate.isDestroyed());
        assertFalse(gate.isActive());
        assertEquals(0.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    public void testDestroyGateDoesNotDoubleDestroy() {
        CachedGate gate = createTestGate();
        gate.setIsDestroyed(true);

        // Should not throw exception or change state
        healthSystem.destroyGate(gate);

        assertTrue(gate.isDestroyed());
    }

    @Test
    @Disabled("Requires Bukkit runtime for Bukkit.broadcast")
    public void testRespawnGateRestoresHealth() {
        CachedGate gate = createTestGate();
        gate.setIsDestroyed(true);
        gate.setHealthCurrent(0.0);
        gate.setHealthMax(100.0);
        gate.setIsActive(false);

        healthSystem.respawnGate(gate);

        assertFalse(gate.isDestroyed());
        assertTrue(gate.isActive());
        assertEquals(100.0, gate.getHealthCurrent(), 0.1);
    }

    @Test
    public void testRespawnGateOnlyWorksForDestroyedGates() {
        CachedGate gate = createTestGate();
        gate.setIsDestroyed(false);
        gate.setHealthCurrent(50.0);

        healthSystem.respawnGate(gate);

        assertFalse(gate.isDestroyed());
        assertEquals(50.0, gate.getHealthCurrent(), 0.1);
    }

    /**
     * Create a test gate with minimal configuration.
     */
    private CachedGate createTestGate() {
        return new CachedGate(
            1,                              // id
            "TestGate",                    // name
            "SLIDING",                     // gateType
            "VERTICAL",                    // motionType
            "PLANE_GRID",                  // geometryDefinitionMode
            60,                            // animationDurationTicks
            1,                             // animationTickRate
            new Vector(0, 0, 0),          // anchorPoint
            5,                             // geometryWidth
            5,                             // geometryHeight
            3,                             // geometryDepth
            100.0,                         // healthCurrent
            100.0,                         // healthMax
            true,                          // isActive
            false,                         // isDestroyed
            false,                         // isInvincible
            90,                            // rotationMaxAngleDegrees
            "north"                        // faceDirection
        );
    }
}

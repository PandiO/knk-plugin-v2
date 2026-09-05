package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.paper.events.GateDoorDamageEvent;
import net.knightsandkings.knk.paper.gates.HealthSystem;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GateDamageConsequenceListener.
 */
class GateDamageConsequenceListenerTest {

    private HealthSystem healthSystem;
    private GateDamageConsequenceListener listener;
    private CachedGate gate;

    @BeforeEach
    void setUp() {
        healthSystem = mock(HealthSystem.class);
        listener = new GateDamageConsequenceListener(healthSystem);

        gate = new CachedGate(
            1, "TestGate", "SLIDING", "VERTICAL", "PLANE_GRID",
            60, 1, new Vector(100, 64, 100), 5, 5, 3,
            500.0, 500.0, true, false, false, 90, "north"
        );
    }

    @Test
    void appliesDamageForEachCause() {
        for (GateDoorDamageEvent.Cause cause : GateDoorDamageEvent.Cause.values()) {
            GateDoorDamageEvent event = new GateDoorDamageEvent(gate, cause, null, null);

            listener.onGateDoorDamage(event);

            verify(healthSystem, atLeastOnce()).applyDamage(eq(gate), eq(10.0));
        }
    }

    @Test
    void skipsAlreadyCancelledEvents() {
        GateDoorDamageEvent event = new GateDoorDamageEvent(gate, GateDoorDamageEvent.Cause.LEFT_CLICK, null, null);
        event.setCancelled(true);

        listener.onGateDoorDamage(event);

        verifyNoInteractions(healthSystem);
    }
}

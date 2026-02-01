package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.domain.gates.CachedGate;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EntityPusherTest {

    @Test
    void shouldPushEntityInFaceDirection() {
        CachedGate gate = new CachedGate(
            1,
            "TestGate",
            "SLIDING",
            "VERTICAL",
            "PLANE_GRID",
            60,
            1,
            new Vector(0, 0, 0),
            1,
            1,
            1,
            500.0,
            500.0,
            true,
            false,
            true,
            90,
            "east"
        );

        Entity entity = mock(Entity.class);

        EntityPusher.pushEntity(entity, gate);

        ArgumentCaptor<Vector> captor = ArgumentCaptor.forClass(Vector.class);
        verify(entity).setVelocity(captor.capture());

        Vector velocity = captor.getValue();
        assertTrue(velocity.getX() > 0, "Expected push to have positive X for east direction");
    }
}

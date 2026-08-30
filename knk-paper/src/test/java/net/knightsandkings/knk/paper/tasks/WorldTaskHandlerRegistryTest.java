package net.knightsandkings.knk.paper.tasks;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorldTaskHandlerRegistryTest {
    @Test
    void startsLocationSelectionHandlerForEntitySpecificFieldName() {
        WorldTaskHandlerRegistry registry = new WorldTaskHandlerRegistry();
        IWorldTaskHandler handler = mock(IWorldTaskHandler.class);
        Player player = mock(Player.class);
        registry.registerHandler("LocationSelection", handler);

        boolean started = registry.startTask(
                player,
                "LocationSelection",
                "AnchorPointId",
                85,
                "{}"
        );

        assertTrue(started);
        verify(handler).startTask(player, 85, "{}");
    }
}
package net.knightsandkings.knk.paper.tasks;

import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LocationTaskHandlerTest {
    @Test
    void pausedTaskDoesNotHandleBlockRightClick() {
        LocationTaskHandler handler = new LocationTaskHandler(
                mock(WorldTasksApi.class),
                mock(Plugin.class)
        );
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        handler.startTask(player, 89, "{}");
        handler.onPlayerChat(player, "pause");

        boolean handled = handler.onBlockRightClick(player, block);

        assertFalse(handled);
        verify(block, never()).getLocation();
    }
}
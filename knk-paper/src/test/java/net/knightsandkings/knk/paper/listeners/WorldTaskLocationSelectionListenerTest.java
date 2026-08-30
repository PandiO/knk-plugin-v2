package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.paper.tasks.LocationTaskHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorldTaskLocationSelectionListenerTest {
    @Test
    void routesMainHandBlockRightClickAndCancelsHandledInteraction() {
        LocationTaskHandler handler = mock(LocationTaskHandler.class);
        WorldTaskLocationSelectionListener listener = new WorldTaskLocationSelectionListener(handler);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        when(handler.onBlockRightClick(player, block)).thenReturn(true);

        listener.onPlayerInteract(event);

        verify(handler).onBlockRightClick(player, block);
        verify(event).setCancelled(true);
    }

    @Test
    void ignoresOffHandBlockRightClick() {
        LocationTaskHandler handler = mock(LocationTaskHandler.class);
        WorldTaskLocationSelectionListener listener = new WorldTaskLocationSelectionListener(handler);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        listener.onPlayerInteract(event);

        verifyNoInteractions(handler);
        verify(event, never()).setCancelled(true);
    }

    @Test
    void leavesInteractionUncancelledWhenNoLocationTaskHandlesIt() {
        LocationTaskHandler handler = mock(LocationTaskHandler.class);
        WorldTaskLocationSelectionListener listener = new WorldTaskLocationSelectionListener(handler);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        when(handler.onBlockRightClick(player, block)).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(handler).onBlockRightClick(player, block);
        verify(event, never()).setCancelled(true);
    }
}
package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.BlockSnapshot;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import net.knightsandkings.knk.paper.events.GateDoorDamageEvent;
import net.knightsandkings.knk.paper.events.GateDoorInteractEvent;
import net.knightsandkings.knk.paper.listeners.GateEventListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GateEventListener: adapting raw Bukkit events into GateDoorHitService lookups
 * and GateDoorInteractEvent/GateDoorDamageEvent firing.
 */
class GateEventListenerTest {

    private GateManager gateManager;
    private GateEventListener listener;
    private CachedGate gate;
    private MockedStatic<Bukkit> bukkitMock;
    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        gateManager = new GateManager();
        listener = new GateEventListener(new GateDoorHitService(gateManager));

        gate = new CachedGate(
            1, "TestGate", "SLIDING", "VERTICAL", "PLANE_GRID",
            60, 1, new Vector(100, 64, 100), 5, 5, 3,
            500.0, 500.0, true, false, false, 90, "north"
        );
        gate.setWorldName("world");
        gate.setUAxis(new Vector(1, 0, 0));
        gate.setVAxis(new Vector(0, 1, 0));
        gate.setNAxis(new Vector(0, 0, 1));
        gate.setMotionVector(new Vector(0, 3, 0));
        gate.setCurrentState(AnimationState.CLOSED);
        gate.setCurrentFrame(0);
        gate.addBlock(new BlockSnapshot(1, new Vector(0, 0, 0), 1, "stone", 0));
        gateManager.cacheGate(gate);

        pluginManager = mock(PluginManager.class);
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    private Block doorBlock() {
        Block block = mock(Block.class);
        when(block.getX()).thenReturn(100);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(100);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(block.getWorld()).thenReturn(world);
        return block;
    }

    private Block unrelatedBlock() {
        Block block = mock(Block.class);
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(block.getWorld()).thenReturn(world);
        return block;
    }

    // ===== BlockBreakEvent =====

    @Test
    void blockBreakOnDoorBlockIsCancelledAndDamagesTheGateForNonAdmin() {
        Block block = doorBlock();
        Player player = mock(Player.class);
        when(player.hasPermission("knk.gate.admin")).thenReturn(false);

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);

        listener.onBlockBreak(event);

        verify(event).setCancelled(true);
        ArgumentCaptor<GateDoorDamageEvent> captor = ArgumentCaptor.forClass(GateDoorDamageEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        assertEquals(GateDoorDamageEvent.Cause.BLOCK_BREAK, captor.getValue().getCause());
        assertSame(gate, captor.getValue().getGate());
    }

    @Test
    void blockBreakOnDoorBlockIsAllowedForAdmin() {
        Block block = doorBlock();
        Player player = mock(Player.class);
        when(player.hasPermission("knk.gate.admin")).thenReturn(true);

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);

        listener.onBlockBreak(event);

        verify(event, never()).setCancelled(true);
        verify(pluginManager, never()).callEvent(any());
    }

    @Test
    void blockBreakOnUnrelatedBlockIsIgnored() {
        Block block = unrelatedBlock();
        Player player = mock(Player.class);

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);

        listener.onBlockBreak(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(pluginManager, never()).callEvent(any());
    }

    // ===== EntityExplodeEvent / BlockExplodeEvent =====

    @Test
    void entityExplodeProtectsDoorBlockAndDamagesTheGate() {
        Block block = doorBlock();
        Entity entity = mock(Entity.class);
        List<Block> blockList = new ArrayList<>(List.of(block));

        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        when(event.blockList()).thenReturn(blockList);
        when(event.getEntity()).thenReturn(entity);

        listener.onEntityExplode(event);

        assertTrue(blockList.isEmpty());
        ArgumentCaptor<GateDoorDamageEvent> captor = ArgumentCaptor.forClass(GateDoorDamageEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        assertEquals(GateDoorDamageEvent.Cause.EXPLOSION, captor.getValue().getCause());
        assertSame(entity, captor.getValue().getCausingEntity());
    }

    @Test
    void blockExplodeProtectsDoorBlockWithNoCausingEntity() {
        Block block = doorBlock();
        List<Block> blockList = new ArrayList<>(List.of(block));

        BlockExplodeEvent event = mock(BlockExplodeEvent.class);
        when(event.blockList()).thenReturn(blockList);

        listener.onBlockExplode(event);

        assertTrue(blockList.isEmpty());
        ArgumentCaptor<GateDoorDamageEvent> captor = ArgumentCaptor.forClass(GateDoorDamageEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        assertNull(captor.getValue().getCausingEntity());
    }

    // ===== ProjectileHitEvent =====

    @Test
    void projectileHitOnDoorBlockDamagesTheGateWithTheShooter() {
        Block block = doorBlock();
        LivingEntity shooter = mock(LivingEntity.class);
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(shooter);

        ProjectileHitEvent event = mock(ProjectileHitEvent.class);
        when(event.getHitBlock()).thenReturn(block);
        when(event.getEntity()).thenReturn(projectile);

        listener.onProjectileHit(event);

        ArgumentCaptor<GateDoorDamageEvent> captor = ArgumentCaptor.forClass(GateDoorDamageEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        assertEquals(GateDoorDamageEvent.Cause.PROJECTILE, captor.getValue().getCause());
        assertSame(shooter, captor.getValue().getCausingEntity());
    }

    @Test
    void projectileHitOnAnEntityIsIgnored() {
        ProjectileHitEvent event = mock(ProjectileHitEvent.class);
        when(event.getHitBlock()).thenReturn(null);

        listener.onProjectileHit(event);

        verify(pluginManager, never()).callEvent(any());
    }

    // ===== PlayerInteractEvent =====

    @Test
    void rightClickOnDoorFiresInteractEventAndPropagatesCancellation() {
        Block block = doorBlock();
        Player player = mock(Player.class);
        when(player.hasPermission("knk.gate.open.*")).thenReturn(true);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        doAnswer(inv -> {
            GateDoorInteractEvent fired = inv.getArgument(0);
            fired.setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(GateDoorInteractEvent.class));

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
    }

    @Test
    void rightClickWithoutPermissionIsDenied() {
        Block block = doorBlock();
        Player player = mock(Player.class);
        when(player.hasPermission("knk.gate.open.*")).thenReturn(false);
        when(player.hasPermission("knk.gate.close.*")).thenReturn(false);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(pluginManager, never()).callEvent(any());
    }

    @Test
    void leftClickOnDoorFiresDamageEvent() {
        Block block = doorBlock();
        Player player = mock(Player.class);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

        listener.onPlayerInteract(event);

        ArgumentCaptor<GateDoorDamageEvent> captor = ArgumentCaptor.forClass(GateDoorDamageEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        assertEquals(GateDoorDamageEvent.Cause.LEFT_CLICK, captor.getValue().getCause());
        assertSame(player, captor.getValue().getCausingEntity());
    }
}

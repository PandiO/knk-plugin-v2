package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.paper.enchantment.FrozenPlayerTracker;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreezeMovementListenerTest {

    @Test
    void blocksMovementWhenPlayerIsFrozenAndPositionChanges() {
        UUID playerId = UUID.randomUUID();
        FrozenPlayerTracker tracker = new TestFrozenPlayerTracker(playerId, true);
        FreezeMovementListener listener = new FreezeMovementListener(tracker);

        Player player = testPlayer(playerId);
        World world = testWorld();
        Location from = new Location(world, 10, 64, 10, 0, 0);
        Location to = new Location(world, 11, 64, 10, 30, 10);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        listener.onPlayerMove(event);

        assertEquals(from, event.getTo());
    }

    @Test
    void allowsCameraRotationWhenBlockPositionDoesNotChange() {
        UUID playerId = UUID.randomUUID();
        FrozenPlayerTracker tracker = new TestFrozenPlayerTracker(playerId, true);
        FreezeMovementListener listener = new FreezeMovementListener(tracker);

        Player player = testPlayer(playerId);
        World world = testWorld();
        Location from = new Location(world, 10, 64, 10, 0, 0);
        Location to = new Location(world, 10, 64, 10, 180, 20);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        listener.onPlayerMove(event);

        assertEquals(to, event.getTo());
    }

    private static Player testPlayer(UUID playerId) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return playerId;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class[]{World.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class TestFrozenPlayerTracker extends FrozenPlayerTracker {
        private final UUID expectedPlayerId;
        private final boolean frozen;

        private TestFrozenPlayerTracker(UUID expectedPlayerId, boolean frozen) {
            super((Plugin) Proxy.newProxyInstance(
                    Plugin.class.getClassLoader(),
                    new Class[]{Plugin.class},
                    (proxy, method, args) -> null
            ));
            this.expectedPlayerId = expectedPlayerId;
            this.frozen = frozen;
        }

        @Override
        public boolean isFrozen(UUID playerId) {
            return frozen && expectedPlayerId.equals(playerId);
        }
    }
}

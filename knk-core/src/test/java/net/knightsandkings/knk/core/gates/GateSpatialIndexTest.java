package net.knightsandkings.knk.core.gates;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GateSpatialIndex.
 */
class GateSpatialIndexTest {

    @Test
    void lookupReturnsNullWhenCellIsEmpty() {
        GateSpatialIndex index = new GateSpatialIndex();

        assertNull(index.lookup("world", 1, 2, 3));
    }

    @Test
    void putThenLookupResolvesTheGate() {
        GateSpatialIndex index = new GateSpatialIndex();

        index.put("world", new Vector(10, 64, 10), 5);

        assertEquals(5, index.lookup("world", 10, 64, 10));
    }

    @Test
    void sameCoordinatesInDifferentWorldsResolveIndependently() {
        GateSpatialIndex index = new GateSpatialIndex();

        index.put("world_nether", new Vector(10, 64, 10), 5);

        assertEquals(5, index.lookup("world_nether", 10, 64, 10));
        assertNull(index.lookup("world", 10, 64, 10));
    }

    @Test
    void removeClearsTheCell() {
        GateSpatialIndex index = new GateSpatialIndex();
        Vector position = new Vector(10, 64, 10);

        index.put("world", position, 5);
        index.remove("world", position);

        assertNull(index.lookup("world", 10, 64, 10));
    }

    @Test
    void moveClearsOldCellAndPopulatesNewCell() {
        GateSpatialIndex index = new GateSpatialIndex();
        Vector oldPosition = new Vector(10, 64, 10);
        Vector newPosition = new Vector(10, 65, 10);

        index.put("world", oldPosition, 5);
        index.move("world", oldPosition, newPosition, 5);

        assertNull(index.lookup("world", 10, 64, 10));
        assertEquals(5, index.lookup("world", 10, 65, 10));
    }

    @Test
    void moveToTheSameCellIsANoOp() {
        GateSpatialIndex index = new GateSpatialIndex();
        Vector position = new Vector(10, 64, 10);

        index.put("world", position, 5);
        // Same block coordinates (only the fractional part differs) - must not clear the cell.
        index.move("world", position, position.clone(), 5);

        assertEquals(5, index.lookup("world", 10, 64, 10));
    }

    @Test
    void putAllAndRemoveAllOperateOnEveryPosition() {
        GateSpatialIndex index = new GateSpatialIndex();
        List<Vector> positions = List.of(new Vector(0, 64, 0), new Vector(1, 64, 0), new Vector(2, 64, 0));

        index.putAll("world", positions, 7);
        assertEquals(7, index.lookup("world", 0, 64, 0));
        assertEquals(7, index.lookup("world", 1, 64, 0));
        assertEquals(7, index.lookup("world", 2, 64, 0));

        index.removeAll("world", positions);
        assertNull(index.lookup("world", 0, 64, 0));
        assertNull(index.lookup("world", 1, 64, 0));
        assertNull(index.lookup("world", 2, 64, 0));
    }

    @Test
    void removeAllForGateOnlyClearsCellsOwnedByThatGate() {
        GateSpatialIndex index = new GateSpatialIndex();
        index.put("world", new Vector(0, 64, 0), 1);
        index.put("world", new Vector(1, 64, 0), 2);

        index.removeAllForGate("world", 1);

        assertNull(index.lookup("world", 0, 64, 0));
        assertEquals(2, index.lookup("world", 1, 64, 0));
    }

    @Test
    void packCellIsCollisionFreeAcrossRealisticCoordinateRanges() {
        // World height limit is -64..320; horizontal range covers a very large map.
        long a = GateSpatialIndex.packCell(30_000_000, -64, -30_000_000);
        long b = GateSpatialIndex.packCell(30_000_000, 320, -30_000_000);
        long c = GateSpatialIndex.packCell(-30_000_000, -64, 30_000_000);
        long d = GateSpatialIndex.packCell(0, 0, 0);

        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(b, c);
        assertNotEquals(b, d);
        assertNotEquals(c, d);
    }
}

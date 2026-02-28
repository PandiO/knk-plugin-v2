package net.knightsandkings.knk.core.util;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateParserTest {

    @Test
    void parseCoordinate_ReturnsVectorForValidJson() {
        Vector result = CoordinateParser.parseCoordinate("{\"x\":1,\"y\":2,\"z\":3}");

        assertNotNull(result);
        assertEquals(1, result.getX(), 0.001);
        assertEquals(2, result.getY(), 0.001);
        assertEquals(3, result.getZ(), 0.001);
    }

    @Test
    void parseCoordinate_ReturnsNullForInvalidJson() {
        Vector result = CoordinateParser.parseCoordinate("{invalid");

        assertNull(result);
    }

    @Test
    void parseCoordinates_ReturnsListForArrayJson() {
        List<Vector> result = CoordinateParser.parseCoordinates("[{\"x\":1,\"y\":2,\"z\":3},{\"x\":4,\"y\":5,\"z\":6}]");

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getX(), 0.001);
        assertEquals(6, result.get(1).getZ(), 0.001);
    }

    @Test
    void parseCoordinateOrFirst_ReturnsFirstArrayElement() {
        Vector result = CoordinateParser.parseCoordinateOrFirst("[{\"x\":7,\"y\":8,\"z\":9}]");

        assertNotNull(result);
        assertEquals(7, result.getX(), 0.001);
        assertEquals(8, result.getY(), 0.001);
        assertEquals(9, result.getZ(), 0.001);
    }
}

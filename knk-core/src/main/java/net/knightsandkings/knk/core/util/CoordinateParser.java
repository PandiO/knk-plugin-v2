package net.knightsandkings.knk.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing coordinate strings from JSON format.
 * Handles single coordinates and arrays of coordinates.
 */
public class CoordinateParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parse a single coordinate from JSON string.
     * Expected format: {"x": 100, "y": 64, "z": 100}
     *
     * @param json JSON string containing coordinates
     * @return Vector representing the coordinate, or null if parsing fails
     */
    public static Vector parseCoordinate(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            JsonNode node = MAPPER.readTree(json);
            if (node.has("x") && node.has("y") && node.has("z")) {
                double x = node.get("x").asDouble();
                double y = node.get("y").asDouble();
                double z = node.get("z").asDouble();
                return new Vector(x, y, z);
            }
        } catch (JsonProcessingException e) {
            // Parsing failed, return null
        }
        
        return null;
    }

    /**
     * Parse an array of coordinates from JSON string.
     * Expected format: [{"x": 100, "y": 64, "z": 100}, {"x": 105, "y": 64, "z": 100}]
     *
     * @param json JSON string containing array of coordinates
     * @return List of Vectors, or empty list if parsing fails
     */
    public static List<Vector> parseCoordinates(String json) {
        List<Vector> result = new ArrayList<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        try {
            JsonNode arrayNode = MAPPER.readTree(json);
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    if (node.has("x") && node.has("y") && node.has("z")) {
                        double x = node.get("x").asDouble();
                        double y = node.get("y").asDouble();
                        double z = node.get("z").asDouble();
                        result.add(new Vector(x, y, z));
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // Parsing failed, return empty list
        }
        
        return result;
    }

    /**
     * Parse a coordinate string that might be a single value or an array.
     * If the string contains an array, returns the first element.
     *
     * @param json JSON string
     * @return Vector or null
     */
    public static Vector parseCoordinateOrFirst(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        // Try single coordinate first
        Vector single = parseCoordinate(json);
        if (single != null) {
            return single;
        }

        // Try array and return first element
        List<Vector> list = parseCoordinates(json);
        return list.isEmpty() ? null : list.get(0);
    }
}

package net.knightsandkings.knk.paper.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

/**
 * Utility class for interpolating placeholder variables in validation messages.
 * 
 * Handles replacement of placeholder variables (e.g., {regionName}, {coordinates})
 * with their actual values from a JsonObject dictionary.
 * 
 * Used by WorldTask handlers to display dynamic validation messages to players.
 */
public class PlaceholderInterpolationUtil {
    private static final Logger LOGGER = Logger.getLogger(PlaceholderInterpolationUtil.class.getName());

    /**
     * Interpolate placeholders in a message template with values from a JsonObject.
     * 
     * @param message The message template containing placeholders (e.g., "Location {coordinates} is outside {regionName}")
     * @param placeholders A JsonObject with placeholder values (e.g., {"coordinates": "(125, 64, -350)", "regionName": "town_springfield"})
     * @return The interpolated message with placeholders replaced by values
     */
    public static String interpolate(String message, JsonObject placeholders) {
        // Null/empty checks
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        if (placeholders == null || placeholders.size() == 0) {
            return message;
        }
        
        String result = message;
        
        // Replace each placeholder
        for (String key : placeholders.keySet()) {
            try {
                JsonElement element = placeholders.get(key);
                
                // Skip null values
                if (element == null || element.isJsonNull()) {
                    LOGGER.fine("Skipping null placeholder: " + key);
                    continue;
                }
                
                // Get string value
                String value = element.getAsString();
                
                // Replace all occurrences of {key} with value
                String placeholder = "{" + key + "}";
                result = result.replace(placeholder, value);
                
                LOGGER.fine("Interpolated placeholder: " + placeholder + " -> " + value);
                
            } catch (Exception e) {
                LOGGER.warning("Error interpolating placeholder '" + key + "': " + e.getMessage());
                // Continue with other placeholders
            }
        }
        
        // Log if there are still unresolved placeholders (debug mode)
        if (result.contains("{") && result.contains("}")) {
            LOGGER.fine("Message still contains unresolved placeholders: " + result);
        }
        
        return result;
    }
    
    /**
     * Merge two JsonObjects containing placeholders.
     * Values from the second object override values from the first if keys conflict.
     * 
     * @param base The base placeholder object
     * @param override The override placeholder object (takes precedence)
     * @return A new JsonObject with merged placeholders
     */
    public static JsonObject mergePlaceholders(JsonObject base, JsonObject override) {
        JsonObject merged = new JsonObject();
        
        // Add all from base
        if (base != null) {
            for (String key : base.keySet()) {
                merged.add(key, base.get(key));
            }
        }
        
        // Override with values from second object
        if (override != null) {
            for (String key : override.keySet()) {
                merged.add(key, override.get(key));
            }
        }
        
        return merged;
    }
}

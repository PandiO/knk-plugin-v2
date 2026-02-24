package net.knightsandkings.knk.paper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class DisplayTextFormatter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private DisplayTextFormatter() {
    }

    public static String translateToLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String legacyFormatted = ChatColor.translateAlternateColorCodes('&', text);
        Component component = LEGACY_SERIALIZER.deserialize(legacyFormatted);
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String legacyFormatted = ChatColor.translateAlternateColorCodes('&', text);
        return LEGACY_SERIALIZER.deserialize(legacyFormatted);
    }
}

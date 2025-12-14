package net.knightsandkings.knk.paper.commands;

import java.util.List;

/**
 * Metadata for a subcommand.
 */
public record CommandMetadata(
        String name,
        String description,
        String usage,
        String permission,
        List<String> examples
) {
    public CommandMetadata(String name, String description, String usage, String permission) {
        this(name, description, usage, permission, List.of());
    }
}

package net.knightsandkings.knk.paper.commands;

import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Registry for knk subcommands with metadata.
 */
public class CommandRegistry {
    private final Map<String, RegisteredCommand> commands = new LinkedHashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public record RegisteredCommand(CommandMetadata metadata, SubcommandExecutor executor) {}

    /**
     * Register a subcommand.
     */
    public void register(CommandMetadata metadata, SubcommandExecutor executor) {
        commands.put(metadata.name().toLowerCase(), new RegisteredCommand(metadata, executor));
    }

    /**
     * Register with aliases.
     */
    public void register(CommandMetadata metadata, SubcommandExecutor executor, String... aliasNames) {
        String primaryName = metadata.name().toLowerCase();
        commands.put(primaryName, new RegisteredCommand(metadata, executor));
        for (String alias : aliasNames) {
            aliases.put(alias.toLowerCase(), primaryName);
        }
    }

    /**
     * Get registered command by name or alias.
     */
    public Optional<RegisteredCommand> get(String nameOrAlias) {
        String key = nameOrAlias.toLowerCase();
        String primary = aliases.getOrDefault(key, key);
        return Optional.ofNullable(commands.get(primary));
    }

    /**
     * List all commands the sender has permission for.
     */
    public List<RegisteredCommand> listAvailable(CommandSender sender) {
        return commands.values().stream()
                .filter(cmd -> cmd.metadata().permission() == null || sender.hasPermission(cmd.metadata().permission()))
                .toList();
    }

    /**
     * List all registered commands.
     */
    public List<RegisteredCommand> listAll() {
        return new ArrayList<>(commands.values());
    }
}

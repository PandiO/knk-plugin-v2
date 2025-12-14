package net.knightsandkings.knk.paper.commands;

import org.bukkit.command.CommandSender;

/**
 * Interface for subcommand executors.
 */
@FunctionalInterface
public interface SubcommandExecutor {
    /**
     * Execute the subcommand.
     * @param sender command sender
     * @param args arguments (excluding subcommand name)
     * @return true if handled
     */
    boolean execute(CommandSender sender, String[] args);
}

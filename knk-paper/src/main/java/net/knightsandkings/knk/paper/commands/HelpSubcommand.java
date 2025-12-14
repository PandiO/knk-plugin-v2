package net.knightsandkings.knk.paper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Help subcommand showing available commands or details for specific subcommand.
 */
public class HelpSubcommand {
    private final CommandRegistry registry;

    public HelpSubcommand(CommandRegistry registry) {
        this.registry = registry;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showCommandList(sender);
        } else {
            showCommandDetail(sender, args[0]);
        }
        return true;
    }

    private void showCommandList(CommandSender sender) {
        List<CommandRegistry.RegisteredCommand> available = registry.listAvailable(sender);
        
        if (available.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No commands available.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "━━━ KnK Commands ━━━");
        for (CommandRegistry.RegisteredCommand cmd : available) {
            sender.sendMessage(ChatColor.AQUA + "/knk " + cmd.metadata().name() + 
                    ChatColor.GRAY + " - " + cmd.metadata().description());
        }
        sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/knk help <command>" + 
                ChatColor.GRAY + " for details");
    }

    private void showCommandDetail(CommandSender sender, String commandName) {
        var cmd = registry.get(commandName);
        
        if (cmd.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown command: " + commandName);
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/knk help" + 
                    ChatColor.GRAY + " to see available commands");
            return;
        }

        CommandMetadata meta = cmd.get().metadata();
        sender.sendMessage(ChatColor.GOLD + "━━━ /knk " + meta.name() + " ━━━");
        sender.sendMessage(ChatColor.GRAY + meta.description());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "Usage:");
        sender.sendMessage(ChatColor.WHITE + "  " + meta.usage());
        
        if (meta.permission() != null) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "Permission: " + ChatColor.GRAY + meta.permission());
        }
        
        if (!meta.examples().isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "Examples:");
            for (String example : meta.examples()) {
                sender.sendMessage(ChatColor.WHITE + "  " + example);
            }
        }
    }
}

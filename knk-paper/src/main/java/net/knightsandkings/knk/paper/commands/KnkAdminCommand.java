package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.ports.api.HealthApi;
import net.knightsandkings.knk.core.ports.api.LocationsQueryApi;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * Root /knk command dispatcher using CommandRegistry.
 */
public class KnkAdminCommand implements CommandExecutor {
    private final CommandRegistry registry = new CommandRegistry();
    private final HelpSubcommand helpSubcommand;

    public KnkAdminCommand(Plugin plugin, HealthApi healthApi, TownsQueryApi townsApi, LocationsQueryApi locationsApi) {
        this.helpSubcommand = new HelpSubcommand(registry);
        
        // Register health
        HealthCommand healthCommand = new HealthCommand(plugin, healthApi);
        registry.register(
                new CommandMetadata("health", "Check API backend health", "/knk health", "knk.admin"),
                (sender, args) -> healthCommand.onCommand(sender, null, "knk", new String[0])
        );
        
        // Register towns
        TownsDebugCommand townsCommand = new TownsDebugCommand(plugin, townsApi);
        registry.register(
                new CommandMetadata("towns", "List or search towns", "/knk towns list [page] [size]", "knk.admin",
                        List.of("/knk towns list", "/knk towns list 1 10")),
                (sender, args) -> townsCommand.onCommand(sender, null, "knk", args)
        );
        
        // Register town (alias for get by ID)
        registry.register(
                new CommandMetadata("town", "Get town details by ID", "/knk town <id>", "knk.admin",
                        List.of("/knk town 1")),
                (sender, args) -> {
                    String[] adjusted = new String[args.length + 1];
                    adjusted[0] = "town";
                    System.arraycopy(args, 0, adjusted, 1, args.length);
                    return townsCommand.onCommand(sender, null, "knk", adjusted);
                }
        );
        
        // Register locations
        LocationsDebugCommand locationsCommand = new LocationsDebugCommand(plugin, locationsApi);
        registry.register(
                new CommandMetadata("locations", "List or get locations", "/knk locations list <page> <size> | /knk locations <id>", "knk.admin",
                        List.of("/knk locations list 1 10", "/knk locations 5")),
                (sender, args) -> locationsCommand.onCommand(sender, null, "knk", args)
        );
        
        // Register location here
        LocationDebugCommand locationHereCommand = new LocationDebugCommand((org.bukkit.plugin.java.JavaPlugin) plugin);
        registry.register(
                new CommandMetadata("location", "Show your current location", "/knk location here", "knk.admin",
                        List.of("/knk location here")),
                (sender, args) -> {
                    if (args.length == 0 || !args[0].equalsIgnoreCase("here")) {
                        sender.sendMessage(ChatColor.YELLOW + "Usage: /knk location here");
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                        return true;
                    }
                    return locationHereCommand.onCommand(sender, null, "knk", new String[0]);
                }
        );
        
        // Register help
        registry.register(
                new CommandMetadata("help", "Show available commands or command details", "/knk help [command]", null,
                        List.of("/knk help", "/knk help towns")),
                (sender, args) -> helpSubcommand.execute(sender, args)
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help when no args
            return helpSubcommand.execute(sender, new String[0]);
        }

        String subcommandName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        var registered = registry.get(subcommandName);
        if (registered.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown command: " + subcommandName);
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/knk help" + 
                    ChatColor.GRAY + " to see available commands");
            return true;
        }

        CommandRegistry.RegisteredCommand cmd = registered.get();
        
        // Check permission
        if (cmd.metadata().permission() != null && !sender.hasPermission(cmd.metadata().permission())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        return cmd.executor().execute(sender, subArgs);
    }
}

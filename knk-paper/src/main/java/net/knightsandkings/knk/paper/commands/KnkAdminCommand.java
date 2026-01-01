package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.ports.api.HealthApi;
import net.knightsandkings.knk.core.ports.api.LocationsQueryApi;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
import net.knightsandkings.knk.core.ports.api.StreetsQueryApi;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import net.knightsandkings.knk.paper.tasks.WorldTaskHandlerRegistry;
import net.knightsandkings.knk.paper.cache.CacheManager;
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

    public KnkAdminCommand(
            Plugin plugin, 
            HealthApi healthApi, 
            TownsQueryApi townsApi, 
            LocationsQueryApi locationsApi, 
            DistrictsQueryApi districtsApi, 
            StreetsQueryApi streetsApi, 
            CacheManager cacheManager,
            WorldTasksApi worldTasksApi,
            WorldTaskHandlerRegistry worldTaskHandlerRegistry,
            String serverId
    ) {
        this.helpSubcommand = new HelpSubcommand(registry);
        
        // Register health
        HealthCommand healthCommand = new HealthCommand(plugin, healthApi);
        registry.register(
                new CommandMetadata("health", "Check API backend health", "/knk health", "knk.admin"),
                (sender, args) -> healthCommand.onCommand(sender, null, "knk", new String[0])
        );
        
        // Register cache command
        if (cacheManager != null) {
            registry.register(
                new CommandMetadata("cache", "View cache statistics and health", "/knk cache", "knk.admin"),
                (sender, args) -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('§', cacheManager.getHealthSummary()));
                    return true;
                }
            );
        }
        
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
        
        // Register districts
        DistrictsDebugCommand districtsCommand = new DistrictsDebugCommand(plugin, districtsApi);
        registry.register(
                new CommandMetadata("districts", "List or search districts", "/knk districts list [page] [size]", "knk.admin",
                        List.of("/knk districts list", "/knk districts list 1 10")),
                (sender, args) -> districtsCommand.onCommand(sender, null, "knk", args)
        );
        
        // Register district (alias for get by ID)
        registry.register(
                new CommandMetadata("district", "Get district details by ID", "/knk district <id>", "knk.admin",
                        List.of("/knk district 1")),
                (sender, args) -> {
                    String[] adjusted = new String[args.length + 1];
                    adjusted[0] = "district";
                    System.arraycopy(args, 0, adjusted, 1, args.length);
                    return districtsCommand.onCommand(sender, null, "knk", adjusted);
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
        
        // Register streets
        StreetsDebugCommand streetsCommand = new StreetsDebugCommand(plugin, streetsApi);
        registry.register(
                new CommandMetadata("streets", "List or search streets", "/knk streets list [page] [size]", "knk.admin",
                        List.of("/knk streets list", "/knk streets list 1 10")),
                (sender, args) -> streetsCommand.onCommand(sender, null, "knk", args)
        );
        
        // Register street (alias for get by ID)
        registry.register(
                new CommandMetadata("street", "Get street details by ID", "/knk street <id>", "knk.admin",
                        List.of("/knk street 1")),
                (sender, args) -> {
                    String[] adjusted = new String[args.length + 1];
                    adjusted[0] = "street";
                    System.arraycopy(args, 0, adjusted, 1, args.length);
                    return streetsCommand.onCommand(sender, null, "knk", adjusted);
                }
        );
        
        // Register task commands
        KnkTaskListCommand taskListCommand = new KnkTaskListCommand(plugin, worldTasksApi);
        registry.register(
                new CommandMetadata("tasks", "List world tasks by status", "/knk tasks [status]", "knk.tasks",
                        List.of("/knk tasks", "/knk tasks Pending", "/knk tasks Claimed")),
                (sender, args) -> taskListCommand.onCommand(sender, null, "knk", args)
        );
        
        KnkTaskClaimCommand taskClaimCommand = new KnkTaskClaimCommand(plugin, worldTasksApi, worldTaskHandlerRegistry, serverId);
        registry.register(
                new CommandMetadata("task-claim", "Claim a world task", "/knk task-claim <id|linkCode>", "knk.tasks",
                        List.of("/knk task-claim 1", "/knk task-claim ABC123")),
                (sender, args) -> taskClaimCommand.onCommand(sender, null, "knk", args)
        );
        
        KnkTaskStatusCommand taskStatusCommand = new KnkTaskStatusCommand(plugin, worldTasksApi);
        registry.register(
                new CommandMetadata("task-status", "Check world task status", "/knk task-status <id|linkCode>", "knk.tasks",
                        List.of("/knk task-status 1", "/knk task-status ABC123")),
                (sender, args) -> taskStatusCommand.onCommand(sender, null, "knk", args)
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

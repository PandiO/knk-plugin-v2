package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.domain.towns.TownSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;

/**
 * Command handler for /knk towns subcommands.
 * Supports:
 *   /knk towns list [page] [size] - List towns (paginated, default page=1, size=5)
 *   /knk town <id> - Get specific town by ID
 *
 * All operations are async with responses scheduled on the main thread.
 */
public class TownsDebugCommand implements CommandExecutor {
    private final Plugin plugin;
    private final TownsQueryApi townsApi;
    
    public TownsDebugCommand(Plugin plugin, TownsQueryApi townsApi) {
        this.plugin = plugin;
        this.townsApi = townsApi;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk towns list [page] [size]  OR  /knk town <id>");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("list")) {
            handleListCommand(sender, args);
            return true;
        } else if (subcommand.equals("town")) {
            handleGetCommand(sender, args);
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Unknown towns subcommand. Usage: /knk towns list [page] [size]  OR  /knk town <id>");
            return true;
        }
    }
    
    private void handleListCommand(CommandSender sender, String[] args) {
        int pageNumber = 1;
        int pageSize = 5;
        
        // Parse optional page and size
        try {
            if (args.length >= 2) {
                pageNumber = Integer.parseInt(args[1]);
            }
            if (args.length >= 3) {
                pageSize = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid page or size. Must be integers.");
            return;
        }
        
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 5; // Reasonable limits
        
        sender.sendMessage(ChatColor.GRAY + "Fetching towns (page " + pageNumber + ", size " + pageSize + ")...");
        
        PagedQuery query = new PagedQuery(pageNumber, pageSize, null, null, false, Collections.emptyMap());
        
        // Execute async search
        townsApi.search(query).thenAccept(result -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Towns (" + result.items().size() + " of " + result.totalCount() + "):");
                
                for (TownSummary town : result.items()) {
                    String name = town.name() != null ? town.name() : "<unnamed>";
                    String desc = town.description() != null ? town.description() : "(no description)";
                    String region = town.wgRegionId() != null ? town.wgRegionId() : "-";
                    
                    sender.sendMessage(ChatColor.GRAY + "  [" + ChatColor.AQUA + town.id() + ChatColor.GRAY + "] " +
                            ChatColor.WHITE + name + ChatColor.GRAY + " - " + desc);
                    sender.sendMessage(ChatColor.GRAY + "       Region: " + region);
                }
                
                if (result.items().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  (no towns)");
                }
                
                // Info bar
                sender.sendMessage(ChatColor.GRAY + "─────────────────");
                sender.sendMessage(ChatColor.GRAY + "Page " + result.pageNumber() + " of " + 
                        (int) Math.ceil((double) result.totalCount() / result.pageSize()) + " | " +
                        "Use: /knk towns list [page#] [size#]");
            });
        }).exceptionally(ex -> {
            // Schedule error message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "✗ Failed to fetch towns");
                
                Throwable cause = ex.getCause();
                
                if (cause instanceof ApiException apiEx) {
                    if (apiEx.getStatusCode() > 0) {
                        sender.sendMessage(ChatColor.RED + "  HTTP " + apiEx.getStatusCode());
                        sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        if (apiEx.getResponseBody() != null && !apiEx.getResponseBody().isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "  Response: " + apiEx.getResponseBody());
                        }
                    } else if (apiEx.getCause() != null) {
                        String rootExceptionName = apiEx.getCause().getClass().getSimpleName();
                        sender.sendMessage(ChatColor.RED + "  Error: " + rootExceptionName);
                        sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        sender.sendMessage(ChatColor.RED + "  " + apiEx.getCause().getMessage());
                    } else {
                        sender.sendMessage(ChatColor.RED + "  " + apiEx.getMessage());
                        if (apiEx.getRequestUrl() != null) {
                            sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "  " + (cause != null ? cause.getMessage() : ex.getMessage()));
                }
                
                plugin.getLogger().warning("Towns list failed: " + ex.getMessage());
                if (cause != null) {
                    plugin.getLogger().warning("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                }
            });
            return null;
        });
    }
    
    private void handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk town <id>");
            return;
        }
        
        int townId;
        try {
            townId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid town ID. Must be a number.");
            return;
        }
        
        sender.sendMessage(ChatColor.GRAY + "Fetching town " + townId + "...");
        
        // Execute async get by ID
        townsApi.getById(townId).thenAccept(town -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Town Details:");
                sender.sendMessage(ChatColor.GRAY + "  ID: " + ChatColor.AQUA + town.id());
                sender.sendMessage(ChatColor.GRAY + "  Name: " + ChatColor.WHITE + (town.name() != null ? town.name() : "<unnamed>"));
                sender.sendMessage(ChatColor.GRAY + "  Description: " + ChatColor.WHITE + 
                        (town.description() != null ? town.description() : "(none)"));
                
                if (town.createdAt() != null) {
                    sender.sendMessage(ChatColor.GRAY + "  Created: " + ChatColor.WHITE + town.createdAt());
                }
                
                sender.sendMessage(ChatColor.GRAY + "  Region: " + ChatColor.AQUA + 
                        (town.wgRegionId() != null ? town.wgRegionId() : "-"));
                sender.sendMessage(ChatColor.GRAY + "  Entry: " + ChatColor.WHITE + 
                        (town.allowEntry() != null && town.allowEntry() ? "✓ Allowed" : "✗ Denied"));
                sender.sendMessage(ChatColor.GRAY + "  Exit: " + ChatColor.WHITE + 
                        (town.allowExit() != null && town.allowExit() ? "✓ Allowed" : "✗ Denied"));
                
                if (town.location() != null) {
                    TownDetail.Location loc = town.location();
                    sender.sendMessage(ChatColor.GRAY + "  Location: " + ChatColor.AQUA + 
                            (loc.name() != null ? loc.name() : "(unnamed)") + ChatColor.GRAY + " @ " +
                            ChatColor.WHITE + loc.x() + ", " + loc.y() + ", " + loc.z() +
                            ChatColor.GRAY + " [" + (loc.world() != null ? loc.world() : "unknown") + "]");
                }
                
                if (town.streets() != null && !town.streets().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Streets (" + town.streets().size() + "):");
                    for (TownDetail.TownStreet street : town.streets()) {
                        sender.sendMessage(ChatColor.GRAY + "    - " + ChatColor.WHITE + 
                                (street.name() != null ? street.name() : "<unnamed>"));
                    }
                }
                
                if (town.districts() != null && !town.districts().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Districts (" + town.districts().size() + "):");
                    for (TownDetail.TownDistrict district : town.districts()) {
                        sender.sendMessage(ChatColor.GRAY + "    - " + ChatColor.WHITE + 
                                (district.name() != null ? district.name() : "<unnamed>") + ChatColor.GRAY + 
                                " [" + ChatColor.AQUA + district.wgRegionId() + ChatColor.GRAY + "]");
                    }
                }
            });
        }).exceptionally(ex -> {
            // Schedule error message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "✗ Failed to fetch town " + townId);
                
                Throwable cause = ex.getCause();
                
                if (cause instanceof ApiException apiEx) {
                    if (apiEx.getStatusCode() > 0) {
                        sender.sendMessage(ChatColor.RED + "  HTTP " + apiEx.getStatusCode());
                        sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        if (apiEx.getResponseBody() != null && !apiEx.getResponseBody().isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "  Response: " + apiEx.getResponseBody());
                        }
                    } else if (apiEx.getCause() != null) {
                        String rootExceptionName = apiEx.getCause().getClass().getSimpleName();
                        sender.sendMessage(ChatColor.RED + "  Error: " + rootExceptionName);
                        sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        sender.sendMessage(ChatColor.RED + "  " + apiEx.getCause().getMessage());
                    } else {
                        sender.sendMessage(ChatColor.RED + "  " + apiEx.getMessage());
                        if (apiEx.getRequestUrl() != null) {
                            sender.sendMessage(ChatColor.RED + "  URL: " + apiEx.getRequestUrl());
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "  " + (cause != null ? cause.getMessage() : ex.getMessage()));
                }
                
                plugin.getLogger().warning("Town get failed: " + ex.getMessage());
                if (cause != null) {
                    plugin.getLogger().warning("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                }
            });
            return null;
        });
    }
}

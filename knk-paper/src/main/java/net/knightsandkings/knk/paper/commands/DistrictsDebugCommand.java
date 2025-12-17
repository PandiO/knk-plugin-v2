package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail;
import net.knightsandkings.knk.core.domain.districts.DistrictSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;

/**
 * Command handler for /knk districts subcommands.
 * Supports:
 *   /knk districts list [page] [size] - List districts (paginated, default page=1, size=5)
 *   /knk district <id> - Get specific district by ID
 *
 * All operations are async with responses scheduled on the main thread.
 * READ-ONLY: no create/update/delete commands.
 */
public class DistrictsDebugCommand implements CommandExecutor {
    private final Plugin plugin;
    private final DistrictsQueryApi districtsApi;
    
    public DistrictsDebugCommand(Plugin plugin, DistrictsQueryApi districtsApi) {
        this.plugin = plugin;
        this.districtsApi = districtsApi;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk districts list [page] [size]  OR  /knk district <id>");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("list")) {
            handleListCommand(sender, args);
            return true;
        } else if (subcommand.equals("district")) {
            handleGetCommand(sender, args);
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Unknown districts subcommand. Usage: /knk districts list [page] [size]  OR  /knk district <id>");
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
        if (pageSize < 1 || pageSize > 100) pageSize = 5;
        
        sender.sendMessage(ChatColor.GRAY + "Fetching districts (page " + pageNumber + ", size " + pageSize + ")...");
        
        PagedQuery query = new PagedQuery(pageNumber, pageSize, null, null, false, Collections.emptyMap());
        
        // Execute async search
        districtsApi.search(query).thenAccept(result -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Districts (" + result.items().size() + " of " + result.totalCount() + "):");
                
                for (DistrictSummary district : result.items()) {
                    String name = district.name() != null ? district.name() : "<unnamed>";
                    String desc = district.description() != null ? district.description() : "(no description)";
                    String region = district.wgRegionId() != null ? district.wgRegionId() : "-";
                    String townName = district.townName() != null ? district.townName() : "?";
                    
                    sender.sendMessage(ChatColor.GRAY + "  [" + ChatColor.AQUA + district.id() + ChatColor.GRAY + "] " +
                            ChatColor.WHITE + name + ChatColor.GRAY + " - " + desc);
                    sender.sendMessage(ChatColor.GRAY + "       Town: " + townName + " | Region: " + region);
                }
                
                if (result.items().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  (no districts)");
                }
                
                // Info bar
                sender.sendMessage(ChatColor.GRAY + "─────────────────");
                sender.sendMessage(ChatColor.GRAY + "Page " + result.pageNumber() + " of " + 
                        (int) Math.ceil((double) result.totalCount() / result.pageSize()) + " | " +
                        "Use: /knk districts list [page#] [size#]");
            });
        }).exceptionally(ex -> {
            // Schedule error message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "✗ Failed to fetch districts");
                
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
                
                plugin.getLogger().warning("Districts list failed: " + ex.getMessage());
                if (cause != null) {
                    plugin.getLogger().warning("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                }
            });
            return null;
        });
    }
    
    private void handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk district <id>");
            return;
        }
        
        int districtId;
        try {
            districtId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid district ID. Must be a number.");
            return;
        }
        
        sender.sendMessage(ChatColor.GRAY + "Fetching district " + districtId + "...");
        
        // Execute async get by ID
        districtsApi.getById(districtId).thenAccept(district -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "District Details:");
                sender.sendMessage(ChatColor.GRAY + "  ID: " + ChatColor.AQUA + district.id());
                sender.sendMessage(ChatColor.GRAY + "  Name: " + ChatColor.WHITE + (district.name() != null ? district.name() : "<unnamed>"));
                sender.sendMessage(ChatColor.GRAY + "  Description: " + ChatColor.WHITE + 
                        (district.description() != null ? district.description() : "(none)"));
                
                if (district.createdAt() != null) {
                    sender.sendMessage(ChatColor.GRAY + "  Created: " + ChatColor.WHITE + district.createdAt());
                }
                
                sender.sendMessage(ChatColor.GRAY + "  Region: " + ChatColor.AQUA + 
                        (district.wgRegionId() != null ? district.wgRegionId() : "-"));
                sender.sendMessage(ChatColor.GRAY + "  Entry: " + ChatColor.WHITE + 
                        (district.allowEntry() != null && district.allowEntry() ? "✓ Allowed" : "✗ Denied"));
                sender.sendMessage(ChatColor.GRAY + "  Exit: " + ChatColor.WHITE + 
                        (district.allowExit() != null && district.allowExit() ? "✓ Allowed" : "✗ Denied"));
                
                if (district.town() != null) {
                    DistrictDetail.Town town = district.town();
                    sender.sendMessage(ChatColor.GRAY + "  Town: " + ChatColor.WHITE + 
                            (town.name() != null ? town.name() : "<unnamed>") + 
                            ChatColor.GRAY + " [" + town.id() + "]");
                }
                
                if (district.location() != null) {
                    DistrictDetail.Location loc = district.location();
                    sender.sendMessage(ChatColor.GRAY + "  Location: " + ChatColor.AQUA + 
                            (loc.name() != null ? loc.name() : "(unnamed)") + ChatColor.GRAY + " @ " +
                            ChatColor.WHITE + loc.x() + ", " + loc.y() + ", " + loc.z() +
                            ChatColor.GRAY + " [" + (loc.world() != null ? loc.world() : "unknown") + "]");
                }
                
                if (district.streets() != null && !district.streets().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Streets (" + district.streets().size() + "):");
                    for (DistrictDetail.Street street : district.streets()) {
                        sender.sendMessage(ChatColor.GRAY + "    - " + ChatColor.WHITE + 
                                (street.name() != null ? street.name() : "<unnamed>"));
                    }
                }
                
                if (district.structures() != null && !district.structures().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Structures (" + district.structures().size() + "):");
                    for (DistrictDetail.Structure structure : district.structures()) {
                        sender.sendMessage(ChatColor.GRAY + "    - " + ChatColor.WHITE + 
                                (structure.name() != null ? structure.name() : "<unnamed>") +
                                (structure.houseNumber() != null ? " #" + structure.houseNumber() : ""));
                    }
                }
            });
        }).exceptionally(ex -> {
            // Schedule error message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "✗ Failed to fetch district");
                
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
                
                plugin.getLogger().warning("District get failed: " + ex.getMessage());
                if (cause != null) {
                    plugin.getLogger().warning("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                }
            });
            return null;
        });
    }
}

package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.streets.*;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.StreetsQueryApi;
import net.knightsandkings.knk.paper.utils.DisplayTextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;

/**
 * Command handler for /knk streets subcommands.
 * READ-ONLY: supports list and getById only.
 * Supports:
 *   /knk streets list [page] [size] - List streets (paginated, default page=1, size=5)
 *   /knk street <id> - Get specific street by ID
 *
 * All operations are async with responses scheduled on the main thread.
 */
public class StreetsDebugCommand implements CommandExecutor {
    private final Plugin plugin;
    private final StreetsQueryApi streetsApi;

    public StreetsDebugCommand(Plugin plugin, StreetsQueryApi streetsApi) {
        this.plugin = plugin;
        this.streetsApi = streetsApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk streets list [page] [size]  OR  /knk street <id>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("list")) {
            handleListCommand(sender, args);
            return true;
        } else if (subcommand.equals("street")) {
            handleGetCommand(sender, args);
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Unknown streets subcommand. Usage: /knk streets list [page] [size]  OR  /knk street <id>");
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

        sender.sendMessage(ChatColor.GRAY + "Fetching streets (page " + pageNumber + ", size " + pageSize + ")...");

        PagedQuery query = new PagedQuery(pageNumber, pageSize, null, null, false, Collections.emptyMap());

        // Execute async search
        streetsApi.search(query).thenAccept(result -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Streets (" + result.items().size() + " of " + result.totalCount() + "):");

                for (StreetSummary street : result.items()) {
                    String name = formatText(street.name(), "<unnamed>");

                    sender.sendMessage(ChatColor.GRAY + "  [" + ChatColor.AQUA + street.id() + ChatColor.GRAY + "] " +
                            ChatColor.WHITE + name);
                }

                if (result.items().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  (no streets)");
                }

                sender.sendMessage(ChatColor.GRAY + "Page " + result.pageNumber() + " of " +
                        Math.max(1, (int) Math.ceil((double) result.totalCount() / result.pageSize())));
            });
        }).exceptionally(throwable -> {
            // Schedule on main thread to send error
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable.getCause() instanceof ApiException) {
                    ApiException apiEx = (ApiException) throwable.getCause();
                    sender.sendMessage(ChatColor.RED + "API error: " + apiEx.getMessage());
                } else {
                    sender.sendMessage(ChatColor.RED + "Error: " + throwable.getMessage());
                }
            });
            return null;
        });
    }

    private void handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /knk street <id>");
            return;
        }

        int streetId;
        try {
            streetId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid street ID. Must be an integer.");
            return;
        }

        sender.sendMessage(ChatColor.GRAY + "Fetching street #" + streetId + "...");

        // Execute async getById
        streetsApi.getById(streetId).thenAccept(street -> {
            // Schedule on main thread to send messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                String name = formatText(street.name(), "<unnamed>");

                sender.sendMessage(ChatColor.GREEN + "Street Details:");
                sender.sendMessage(ChatColor.GRAY + "  ID: " + ChatColor.AQUA + street.id());
                sender.sendMessage(ChatColor.GRAY + "  Name: " + ChatColor.WHITE + name);

                // District IDs
                if (street.districtIds() != null && !street.districtIds().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  District IDs: " + ChatColor.YELLOW + street.districtIds());
                } else {
                    sender.sendMessage(ChatColor.GRAY + "  District IDs: (none)");
                }

                // Districts
                if (street.districts() != null && !street.districts().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Districts:");
                    for (StreetDistrict district : street.districts()) {
                        String districtName = district.name() != null ? district.name() : "<unnamed>";
                        districtName = formatText(districtName, "<unnamed>");
                        String wgRegion = district.wgRegionId() != null ? district.wgRegionId() : "-";
                        sender.sendMessage(ChatColor.GRAY + "    [" + ChatColor.AQUA + district.id() + ChatColor.GRAY + "] " +
                                ChatColor.WHITE + districtName + ChatColor.GRAY + " (WG: " + wgRegion + ")");
                    }
                } else {
                    sender.sendMessage(ChatColor.GRAY + "  Districts: (none)");
                }

                // Structures
                if (street.structures() != null && !street.structures().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  Structures:");
                    for (StreetStructure structure : street.structures()) {
                        String structureName = structure.name() != null ? structure.name() : "<unnamed>";
                        structureName = formatText(structureName, "<unnamed>");
                        String houseNum = structure.houseNumber() != null ? "#" + structure.houseNumber() : "-";
                        sender.sendMessage(ChatColor.GRAY + "    [" + ChatColor.AQUA + structure.id() + ChatColor.GRAY + "] " +
                                ChatColor.WHITE + structureName + ChatColor.GRAY + " (" + houseNum + ")");
                    }
                } else {
                    sender.sendMessage(ChatColor.GRAY + "  Structures: (none)");
                }
            });
        }).exceptionally(throwable -> {
            // Schedule on main thread to send error
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable.getCause() instanceof ApiException) {
                    ApiException apiEx = (ApiException) throwable.getCause();
                    sender.sendMessage(ChatColor.RED + "API error: " + apiEx.getMessage());
                    if (apiEx.getStatusCode() == 404) {
                        sender.sendMessage(ChatColor.GRAY + "Street #" + streetId + " not found.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Error: " + throwable.getMessage());
                }
            });
            return null;
        });
    }

    private String formatText(String value, String fallback) {
        String raw = (value == null || value.isBlank()) ? fallback : value;
        return DisplayTextFormatter.translateToLegacy(raw);
    }
}

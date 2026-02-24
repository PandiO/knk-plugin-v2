package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.location.KnkLocation;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.LocationsQueryApi;
import net.knightsandkings.knk.paper.utils.DisplayTextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class LocationsDebugCommand implements CommandExecutor {
    private final Plugin plugin;
    private final LocationsQueryApi locationsApi;

    public LocationsDebugCommand(Plugin plugin, LocationsQueryApi locationsApi) {
        this.plugin = plugin;
        this.locationsApi = locationsApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " list <page> <size> | /" + label + " <id>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            int page = args.length > 1 ? parseInt(args[1], 1) : 1;
            int size = args.length > 2 ? parseInt(args[2], 10) : 10;

            sender.sendMessage(ChatColor.GRAY + "Fetching locations page " + page + ", size " + size + "...");
            PagedQuery query = new PagedQuery(page, size, null, null, false, null);
            locationsApi.search(query)
                    .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> printLocationsPage(sender, result)))
                    .exceptionally(ex -> {
                        Bukkit.getScheduler().runTask(plugin, () -> printError(sender, ex));
                        return null;
                    });
            return true;
        }

        // Assume ID lookup
        int id = parseInt(args[0], -1);
        if (id <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid id. Usage: /" + label + " <id>");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Fetching location id=" + id + "...");
        locationsApi.getById(id)
                .thenAccept(loc -> Bukkit.getScheduler().runTask(plugin, () -> printLocation(sender, loc)))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> printError(sender, ex));
                    return null;
                });
        return true;
    }

    private void printLocationsPage(CommandSender sender, Page<KnkLocation> page) {
        sender.sendMessage(ChatColor.GREEN + "Locations: " + page.items().size() + " items (page " + page.pageNumber() + ", total " + page.totalCount() + ")");
        for (KnkLocation loc : page.items()) {
            sender.sendMessage(ChatColor.GRAY + " - id=" + loc.id() + ", world=" + loc.world() +
                    ", x=" + loc.x() + ", y=" + loc.y() + ", z=" + loc.z());
        }
    }

    private void printLocation(CommandSender sender, KnkLocation loc) {
        if (loc == null) {
            sender.sendMessage(ChatColor.YELLOW + "No location found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Location:");
        sender.sendMessage(ChatColor.GRAY + " id=" + loc.id());
        sender.sendMessage(ChatColor.GRAY + " name=" + formatText(loc.name(), "-"));
        sender.sendMessage(ChatColor.GRAY + " world=" + loc.world());
        sender.sendMessage(ChatColor.GRAY + " x=" + loc.x() + ", y=" + loc.y() + ", z=" + loc.z());
        sender.sendMessage(ChatColor.GRAY + " yaw=" + loc.yaw() + ", pitch=" + loc.pitch());
    }

    private void printError(CommandSender sender, Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ApiException apiEx) {
            if (apiEx.getStatusCode() > 0) {
                sender.sendMessage(ChatColor.RED + "HTTP " + apiEx.getStatusCode());
                if (apiEx.getRequestUrl() != null) sender.sendMessage(ChatColor.RED + "URL: " + apiEx.getRequestUrl());
                if (apiEx.getResponseBody() != null) sender.sendMessage(ChatColor.RED + apiEx.getResponseBody());
            } else if (apiEx.getCause() != null) {
                sender.sendMessage(ChatColor.RED + apiEx.getCause().getClass().getSimpleName() + ": " + apiEx.getCause().getMessage());
            } else {
                sender.sendMessage(ChatColor.RED + apiEx.getMessage());
            }
        } else {
            sender.sendMessage(ChatColor.RED + (cause != null ? cause.getMessage() : ex.getMessage()));
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String formatText(String value, String fallback) {
        String raw = (value == null || value.isBlank()) ? fallback : value;
        return DisplayTextFormatter.translateToLegacy(raw);
    }
}

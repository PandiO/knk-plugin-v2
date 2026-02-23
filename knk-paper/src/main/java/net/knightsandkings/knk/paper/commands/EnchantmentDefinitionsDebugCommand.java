package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.dataaccess.EnchantmentDefinitionsDataAccess;
import net.knightsandkings.knk.core.dataaccess.FetchResult;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.enchantments.KnkEnchantmentDefinition;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.paper.mapper.EnchantmentDefinitionBukkitMapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Command handler for /knk enchantments.
 *
 * Supports:
 * - /knk enchantments list [page] [size]
 * - /knk enchantments search <id|key|displayName> <value> [page] [size]
 * - /knk enchantments apply <id> [level]
 */
public class EnchantmentDefinitionsDebugCommand implements CommandExecutor {
    private final Plugin plugin;
    private final EnchantmentDefinitionsDataAccess dataAccess;

    public EnchantmentDefinitionsDebugCommand(Plugin plugin, EnchantmentDefinitionsDataAccess dataAccess) {
        this.plugin = plugin;
        this.dataAccess = dataAccess;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk enchantments list [page] [size] | " +
                    "/knk enchantments search <id|key|displayName> <value> [page] [size] | " +
                    "/knk enchantments apply <id> [level]");
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "list" -> {
                handleList(sender, args);
                yield true;
            }
            case "search" -> {
                handleSearch(sender, args);
                yield true;
            }
            case "apply" -> {
                handleApply(sender, args);
                yield true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand. Use: list, search, apply");
                yield true;
            }
        };
    }

    private void handleList(CommandSender sender, String[] args) {
        int page = args.length > 1 ? parseInt(args[1], 1) : 1;
        int size = args.length > 2 ? parseInt(args[2], 10) : 10;
        page = Math.max(1, page);
        size = Math.max(1, Math.min(size, 100));

        sender.sendMessage(ChatColor.GRAY + "Fetching enchantments (page " + page + ", size " + size + ")...");

        dataAccess.listAsync(page, size)
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> printPage(sender, result)))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> printError(sender, ex));
                    return null;
                });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk enchantments search <id|key|displayName> <value> [page] [size]");
            return;
        }

        String fieldInput = args[1];
        String valueInput = args[2];
        int page = args.length > 3 ? parseInt(args[3], 1) : 1;
        int size = args.length > 4 ? parseInt(args[4], 10) : 10;
        page = Math.max(1, page);
        size = Math.max(1, Math.min(size, 100));

        Map<String, String> filters = buildSearchFilters(fieldInput, valueInput);
        if (filters == null) {
            sender.sendMessage(ChatColor.RED + "Invalid field. Allowed: id, key, displayName");
            return;
        }

        PagedQuery query = new PagedQuery(page, size, null, null, false, filters);
        sender.sendMessage(ChatColor.GRAY + "Searching enchantments by " + fieldInput + "...");

        dataAccess.searchAsync(query)
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> printPage(sender, result)))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> printError(sender, ex));
                    return null;
                });
    }

    private void handleApply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /knk enchantments apply <id> [level]");
            return;
        }

        int enchantmentDefinitionId = parseInt(args[1], -1);
        if (enchantmentDefinitionId <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid enchantment definition ID.");
            return;
        }

        int requestedLevel = args.length > 2 ? parseInt(args[2], -1) : -1;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "You must hold an item in your main hand.");
            return;
        }

        sender.sendMessage(ChatColor.GRAY + "Fetching enchantment definition " + enchantmentDefinitionId + "...");

        dataAccess.getByIdAsync(enchantmentDefinitionId)
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> applyEnchantment(sender, player, result, requestedLevel)))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> printError(sender, ex));
                    return null;
                });
    }

    private void applyEnchantment(CommandSender sender, Player player, FetchResult<KnkEnchantmentDefinition> result, int requestedLevel) {
        KnkEnchantmentDefinition definition = result != null ? result.value().orElse(null) : null;
        if (definition == null) {
            sender.sendMessage(ChatColor.RED + "Enchantment definition not found.");
            return;
        }

        EnchantmentDefinitionBukkitMapper.BukkitEnchantmentResolution resolution = EnchantmentDefinitionBukkitMapper.toBukkit(definition);
        if (!resolution.isValid()) {
            sender.sendMessage(ChatColor.RED + "Unable to apply enchantment: " + resolution.error());
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "You are no longer holding a valid item.");
            return;
        }

        int maxLevel = definition.maxLevel() != null ? Math.max(1, definition.maxLevel()) : resolution.defaultLevel();
        int level = requestedLevel > 0 ? requestedLevel : resolution.defaultLevel();
        level = Math.min(level, maxLevel);

        heldItem.addUnsafeEnchantment(resolution.enchantment(), level);
        player.getInventory().setItemInMainHand(heldItem);

        sender.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.AQUA + resolution.namespaceKey() +
                ChatColor.GREEN + " level " + ChatColor.AQUA + level +
                ChatColor.GREEN + " to your held item.");
    }

    private void printPage(CommandSender sender, Page<KnkEnchantmentDefinition> page) {
        sender.sendMessage(ChatColor.GREEN + "Enchantments (" + page.items().size() + " of " + page.totalCount() + "):");
        for (KnkEnchantmentDefinition definition : page.items()) {
            String namespace = definition.baseEnchantmentNamespaceKey() != null
                    ? definition.baseEnchantmentNamespaceKey()
                    : "-";

            sender.sendMessage(ChatColor.GRAY + "  [" + ChatColor.AQUA + definition.id() + ChatColor.GRAY + "] " +
                    ChatColor.WHITE + safe(definition.key()) + ChatColor.GRAY + " | " +
                    ChatColor.WHITE + safe(definition.displayName()) + ChatColor.GRAY +
                    " | maxLevel=" + (definition.maxLevel() != null ? definition.maxLevel() : 1) +
                    " | base=" + namespace);
        }

        if (page.items().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (no enchantments)");
        }

        int totalPages = page.pageSize() > 0 ? Math.max(1, (int) Math.ceil((double) page.totalCount() / page.pageSize())) : 1;
        sender.sendMessage(ChatColor.GRAY + "Page " + page.pageNumber() + " of " + totalPages);
    }

    private void printError(CommandSender sender, Throwable ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof ApiException apiEx) {
            if (apiEx.getStatusCode() > 0) {
                sender.sendMessage(ChatColor.RED + "HTTP " + apiEx.getStatusCode());
                if (apiEx.getRequestUrl() != null) {
                    sender.sendMessage(ChatColor.RED + "URL: " + apiEx.getRequestUrl());
                }
                if (apiEx.getResponseBody() != null && !apiEx.getResponseBody().isEmpty()) {
                    sender.sendMessage(ChatColor.RED + apiEx.getResponseBody());
                }
            } else if (apiEx.getCause() != null) {
                sender.sendMessage(ChatColor.RED + apiEx.getCause().getClass().getSimpleName() + ": " + apiEx.getCause().getMessage());
            } else {
                sender.sendMessage(ChatColor.RED + apiEx.getMessage());
            }
        } else {
            sender.sendMessage(ChatColor.RED + (cause != null ? cause.getMessage() : ex.getMessage()));
        }
    }

    private Map<String, String> buildSearchFilters(String fieldInput, String valueInput) {
        if (fieldInput == null || valueInput == null) {
            return null;
        }

        String normalized = fieldInput.trim().toLowerCase();
        Map<String, String> filters = new HashMap<>();

        switch (normalized) {
            case "id" -> {
                int idValue = parseInt(valueInput, -1);
                if (idValue <= 0) return null;
                filters.put("Id", String.valueOf(idValue));
                return filters;
            }
            case "key" -> {
                filters.put("Key", valueInput);
                return filters;
            }
            case "displayname", "display_name", "display-name" -> {
                filters.put("DisplayName", valueInput);
                return filters;
            }
            default -> {
                return null;
            }
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }
}

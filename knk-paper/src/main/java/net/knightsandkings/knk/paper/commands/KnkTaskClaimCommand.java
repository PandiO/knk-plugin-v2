package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.api.dto.WorldTaskDto;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import net.knightsandkings.knk.paper.tasks.WorldTaskHandlerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Command: /knk task claim <id|linkCode>
 * Claims a world task and starts the handler for the current player.
 */
public class KnkTaskClaimCommand implements CommandExecutor {
    private final Plugin plugin;
    private final WorldTasksApi worldTasksApi;
    private final WorldTaskHandlerRegistry handlerRegistry;
    private final String serverId;

    public KnkTaskClaimCommand(Plugin plugin, WorldTasksApi worldTasksApi, WorldTaskHandlerRegistry handlerRegistry, String serverId) {
        this.plugin = plugin;
        this.worldTasksApi = worldTasksApi;
        this.handlerRegistry = handlerRegistry;
        this.serverId = serverId;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /knk task claim <id|linkCode>");
            return true;
        }

        String idOrLinkCode = args[0];
        sender.sendMessage(ChatColor.GRAY + "Claiming task: " + idOrLinkCode + "...");

        // Determine if it's an ID or link code
        boolean isNumeric = idOrLinkCode.matches("\\d+");

        var taskFuture = isNumeric 
            ? worldTasksApi.getById(Integer.parseInt(idOrLinkCode))
            : worldTasksApi.getByLinkCode(idOrLinkCode);

        taskFuture.thenAccept(task -> {
            if (task == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Task not found: " + idOrLinkCode);
                });
                return;
            }

            // Check if task is already claimed or completed
            if (!"Pending".equals(task.status())) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Task is not available (status: " + task.status() + ")");
                });
                return;
            }

            // Claim the task
            worldTasksApi.claim(task.id(), serverId, player.getName()).thenAccept(claimedTask -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "✓ Task claimed: " + claimedTask.entityType() + "." + claimedTask.fieldName());
                    sender.sendMessage(ChatColor.GRAY + "Link code: " + ChatColor.WHITE + claimedTask.linkCode());

                    // Start the handler if available
                    boolean started = handlerRegistry.startTask(
                        player, 
                        claimedTask.fieldName(), 
                        claimedTask.id(), 
                        claimedTask.inputJson()
                    );

                    if (started) {
                        sender.sendMessage(ChatColor.YELLOW + "Handler started. Follow the prompts to complete the task.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "⚠ No handler registered for field: " + claimedTask.fieldName());
                        sender.sendMessage(ChatColor.GRAY + "You may need to complete this task manually.");
                    }
                });
            }).exceptionally(ex -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Failed to claim task: " + ex.getMessage());
                    plugin.getLogger().warning("Failed to claim task " + task.id() + ": " + ex.getMessage());
                });
                return null;
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "Failed to fetch task: " + ex.getMessage());
                plugin.getLogger().warning("Failed to fetch task: " + ex.getMessage());
            });
            return null;
        });

        return true;
    }
}

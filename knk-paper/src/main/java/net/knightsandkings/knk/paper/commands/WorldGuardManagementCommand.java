package net.knightsandkings.knk.paper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.knightsandkings.knk.paper.tasks.WgRegionIdTaskHandler;

/**
 * Command for managing WorldGuard regions programmatically.
 * Handles region renaming operations typically called by the Web API.
 */
public class WorldGuardManagementCommand implements CommandExecutor {
    private final WgRegionIdTaskHandler regionHandler;

    public WorldGuardManagementCommand(WgRegionIdTaskHandler regionHandler) {
        this.regionHandler = regionHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c[KnK] Usage: /knk wgm rename <oldRegionId> <newRegionId>");
            return false;
        }

        String subcommand = args[0].toLowerCase();

        if ("rename".equals(subcommand)) {
            if (args.length < 3) {
                sender.sendMessage("§c[KnK] Usage: /knk wgm rename <oldRegionId> <newRegionId>");
                return false;
            }
            
            String oldRegionId = args[1];
            String newRegionId = args[2];
            
            boolean success = regionHandler.renameRegion(oldRegionId, newRegionId);
            if (success) {
                sender.sendMessage("§a[KnK] Region renamed from " + oldRegionId + " to " + newRegionId);
            } else {
                sender.sendMessage("§c[KnK] Failed to rename region from " + oldRegionId + " to " + newRegionId);
            }
            return true;
        }

        sender.sendMessage("§c[KnK] Unknown subcommand: " + subcommand);
        return false;
    }
}

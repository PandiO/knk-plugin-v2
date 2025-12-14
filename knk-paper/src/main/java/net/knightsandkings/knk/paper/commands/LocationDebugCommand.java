package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.location.KnkLocation;
import net.knightsandkings.knk.paper.mapper.PaperLocationMapper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LocationDebugCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public LocationDebugCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        KnkLocation loc = PaperLocationMapper.fromBukkit(player.getLocation());
        sender.sendMessage("Location:");
        sender.sendMessage(" world=" + loc.world());
        sender.sendMessage(" x=" + loc.x() + ", y=" + loc.y() + ", z=" + loc.z());
        sender.sendMessage(" yaw=" + loc.yaw() + ", pitch=" + loc.pitch());
        return true;
    }
}

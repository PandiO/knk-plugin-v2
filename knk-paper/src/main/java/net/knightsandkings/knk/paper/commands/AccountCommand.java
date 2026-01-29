package net.knightsandkings.knk.paper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.paper.user.PlayerUserData;
import net.knightsandkings.knk.paper.user.UserManager;

/**
 * Command handler for /account (status).
 * Displays cached account information to the player.
 */
public class AccountCommand implements CommandExecutor {
    private final UserManager userManager;
    private final KnkConfig config;

    public AccountCommand(UserManager userManager, KnkConfig config) {
        this.userManager = userManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        PlayerUserData userData = userManager.getCachedUser(player.getUniqueId());
        if (userData == null) {
            sendPrefixed(player, "&cPlease rejoin the server and try again");
            return true;
        }

        sendPrefixed(player, "&6=== Your Account ===");
        sendRaw(player, "");
        sendRaw(player, "  &eUsername: &f" + userData.username());
        sendRaw(player, "  &eUUID: &7" + userData.uuid());

        String emailValue = userData.hasEmailLinked()
            ? "&a" + (userData.email() != null ? userData.email() : "Linked")
            : "&cNot linked";
        sendRaw(player, "  &eEmail: " + emailValue);

        sendRaw(player, "");
        sendRaw(player, "  &6Coins: &e" + safeInt(userData.coins()));
        sendRaw(player, "  &bGems: &3" + safeInt(userData.gems()));
        sendRaw(player, "  &dExperience: &5" + safeInt(userData.experiencePoints()));
        sendRaw(player, "");

        if (!userData.hasEmailLinked()) {
            sendRaw(player, "  &7Use &6/account create &7or &6/account link &7to link email");
        }

        if (userData.hasDuplicateAccount()) {
            sendRaw(player, "  &c⚠ Duplicate account detected!");
            sendRaw(player, "  &7Use &6/account link &7to resolve");
        }

        return true;
    }

    private void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(colorize(config.messages().prefix() + message));
    }

    private void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
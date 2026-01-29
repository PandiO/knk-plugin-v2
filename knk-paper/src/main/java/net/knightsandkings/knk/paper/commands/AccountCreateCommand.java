package net.knightsandkings.knk.paper.commands;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.knightsandkings.knk.api.dto.ChangePasswordRequestDto;
import net.knightsandkings.knk.core.ports.api.UserAccountApi;
import net.knightsandkings.knk.paper.KnKPlugin;
import net.knightsandkings.knk.paper.chat.ChatCaptureManager;
import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.paper.user.PlayerUserData;
import net.knightsandkings.knk.paper.user.UserManager;

/**
 * Command handler for /account create.
 * Starts chat capture flow and updates email/password via API.
 */
public class AccountCreateCommand implements CommandExecutor {
    private final KnKPlugin plugin;
    private final UserManager userManager;
    private final ChatCaptureManager chatCaptureManager;
    private final UserAccountApi userAccountApi;
    private final KnkConfig config;

    public AccountCreateCommand(
        KnKPlugin plugin,
        UserManager userManager,
        ChatCaptureManager chatCaptureManager,
        UserAccountApi userAccountApi,
        KnkConfig config
    ) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.chatCaptureManager = chatCaptureManager;
        this.userAccountApi = userAccountApi;
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

        if (userData.hasEmailLinked()) {
            sendPrefixed(player, "&cYou already have an email linked!");
            sendPrefixed(player, "&eUse &6/account &eto view your account");
            return true;
        }

        chatCaptureManager.startAccountCreateFlow(
            player,
            data -> handleAccountCreation(player, userData, data),
            () -> runSync(() -> sendPrefixed(player, "&cAccount creation cancelled"))
        );

        return true;
    }

    private void handleAccountCreation(Player player, PlayerUserData userData, Map<String, String> data) {
        String email = data.get("email");
        String password = data.get("password");

        if (email == null || password == null) {
            runSync(() -> sendPrefixed(player, "&cMissing account details. Please try again."));
            return;
        }

        if (userData.userId() == null) {
            runSync(() -> sendPrefixed(player, "&cAccount not ready yet. Please rejoin and try again."));
            return;
        }

        Integer userId = userData.userId();

        userAccountApi.updateEmail(userId, email)
            .thenCompose(v -> userAccountApi.changePassword(
                userId,
                new ChangePasswordRequestDto("", password, password)
            ))
            .thenRun(() -> runSync(() -> {
                PlayerUserData updated = userData.withEmailLinked(email);
                userManager.updateCachedUser(player.getUniqueId(), updated);
                sendPrefixed(player, config.messages().accountCreated());
            }))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to create account for " + player.getName() + ": " + ex.getMessage());
                runSync(() -> sendPrefixed(player, "&cFailed to create account. Please try again later."));
                return null;
            });
    }

    private void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(colorize(config.messages().prefix() + message));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
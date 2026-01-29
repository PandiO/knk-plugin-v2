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
import net.knightsandkings.knk.paper.utils.CommandCooldownManager;

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
    private final CommandCooldownManager cooldownManager;

    public AccountCreateCommand(
        KnKPlugin plugin,
        UserManager userManager,
        ChatCaptureManager chatCaptureManager,
        UserAccountApi userAccountApi,
        KnkConfig config,
        CommandCooldownManager cooldownManager
    ) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.chatCaptureManager = chatCaptureManager;
        this.userAccountApi = userAccountApi;
        this.config = config;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        // Check cooldown
        int cooldownSeconds = config.account().cooldowns().accountCreateSeconds();
        if (!cooldownManager.canExecute(player.getUniqueId(), "account.create", cooldownSeconds)) {
            int remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), "account.create", cooldownSeconds);
            sendPrefixed(player, "&cPlease wait " + remaining + " seconds before creating another account.");
            plugin.getLogger().fine(player.getName() + " attempted /account create but is on cooldown (" + remaining + "s remaining)");
            return true;
        }

        PlayerUserData userData = userManager.getCachedUser(player.getUniqueId());
        if (userData == null) {
            sendPrefixed(player, "&cPlease rejoin the server and try again");
            plugin.getLogger().warning("Player " + player.getName() + " has no cached user data for /account create");
            return true;
        }

        if (userData.hasEmailLinked()) {
            sendPrefixed(player, "&cYou already have an email linked!");
            sendPrefixed(player, "&eUse &6/account &eto view your account");
            plugin.getLogger().fine(player.getName() + " attempted /account create but already has email linked");
            return true;
        }

        // Log command execution start
        plugin.getLogger().info(player.getName() + " started /account create flow");

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
            plugin.getLogger().warning("Account creation for " + player.getName() + " failed: missing email or password");
            runSync(() -> sendPrefixed(player, "&cMissing account details. Please try again."));
            return;
        }

        if (userData.userId() == null) {
            plugin.getLogger().warning("Account creation for " + player.getName() + " failed: user ID not available");
            runSync(() -> sendPrefixed(player, "&cAccount not ready yet. Please rejoin and try again."));
            return;
        }

        Integer userId = userData.userId();
        plugin.getLogger().info("Processing account creation for " + player.getName() + " (ID: " + userId + ", email: " + email + ")");

        // Record cooldown at start of API calls
        cooldownManager.recordExecution(player.getUniqueId(), "account.create");

        userAccountApi.updateEmail(userId, email)
            .thenCompose(v -> {
                plugin.getLogger().fine("Email updated for " + player.getName() + ", proceeding to password change");
                return userAccountApi.changePassword(
                    userId,
                    new ChangePasswordRequestDto("", password, password)
                );
            })
            .thenRun(() -> runSync(() -> {
                PlayerUserData updated = userData.withEmailLinked(email);
                userManager.updateCachedUser(player.getUniqueId(), updated);
                sendPrefixed(player, config.messages().accountCreated());
                plugin.getLogger().info("Account creation complete for " + player.getName() + " (email: " + email + ")");
            }))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to create account for " + player.getName() + ": " + ex.getMessage());
                if (ex.getCause() != null) {
                    plugin.getLogger().severe("  Cause: " + ex.getCause().getMessage());
                }
                runSync(() -> {
                    sendPrefixed(player, "&cFailed to create account. Please try again later.");
                    // Reset cooldown on failure so player can retry
                    cooldownManager.resetCooldown(player.getUniqueId(), "account.create");
                });
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
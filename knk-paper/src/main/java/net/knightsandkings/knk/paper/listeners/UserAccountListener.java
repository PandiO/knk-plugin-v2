package net.knightsandkings.knk.paper.listeners;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.paper.user.PlayerUserData;
import net.knightsandkings.knk.paper.user.UserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Listener for player join/quit events related to account management.
 * 
 * Responsibilities:
 * - Sync user data on join via UserManager
 * - Display welcome messages with account status
 * - Prompt for account linking if needed
 * - Clear cache on player quit
 * 
 * Priority: HIGH
 * - Runs early in join sequence to ensure user data is available
 * - Must complete before other plugins that depend on user state
 */
public class UserAccountListener implements Listener {
    private final UserManager userManager;
    private final KnkConfig.MessagesConfig messagesConfig;
    private final Logger logger;
    
    public UserAccountListener(
        UserManager userManager,
        KnkConfig.MessagesConfig messagesConfig,
        Logger logger
    ) {
        this.userManager = userManager;
        this.messagesConfig = messagesConfig;
        this.logger = logger;
    }
    
    /**
     * Handle player join - sync user data and display account status.
     * 
     * Priority: HIGH to ensure user data is loaded early in the join sequence.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        logger.info("Player " + player.getName() + " joined, syncing account data...");
        
        try {
            // Sync user data (blocking call - acceptable on join)
            PlayerUserData userData = userManager.onPlayerJoin(player);
            
            // Send welcome message
            sendWelcomeMessage(player, userData);
            
            // Check for duplicate account and prompt if needed
            if (userData.hasDuplicateAccount()) {
                sendDuplicateAccountPrompt(player, userData);
            }
            
            // Check for account without email and suggest linking
            if (!userData.hasEmailLinked() && userData.userId() != null) {
                sendAccountLinkSuggestion(player);
            }
            
        } catch (Exception ex) {
            logger.severe("Failed to process join for " + player.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
            
            // Send fallback message
            player.sendMessage(
                Component.text(messagesConfig.prefix())
                    .append(Component.text("Welcome! Your account data could not be loaded. Please contact an admin if this persists.")
                        .color(NamedTextColor.YELLOW))
            );
        }
    }
    
    /**
     * Handle player quit - clear cached user data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        userManager.clearCachedUser(player.getUniqueId());
        logger.fine("Cleared cache for " + player.getName() + " on quit");
    }
    
    /**
     * Send welcome message with account balance info.
     */
    private void sendWelcomeMessage(Player player, PlayerUserData userData) {
        Component welcomeMsg = Component.text(messagesConfig.prefix())
            .append(Component.text("Welcome back, ")
                .color(NamedTextColor.GREEN))
            .append(Component.text(player.getName())
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD))
            .append(Component.text("!")
                .color(NamedTextColor.GREEN));
        
        player.sendMessage(welcomeMsg);
        
        // Show balance if available
        if (userData.userId() != null) {
            Component balanceMsg = Component.text(messagesConfig.prefix())
                .append(Component.text("Balance: ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(userData.coins() + " coins")
                    .color(NamedTextColor.GOLD))
                .append(Component.text(", ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(userData.gems() + " gems")
                    .color(NamedTextColor.AQUA))
                .append(Component.text(", ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(userData.experiencePoints() + " XP")
                    .color(NamedTextColor.GREEN));
            
            player.sendMessage(balanceMsg);
        }
    }
    
    /**
     * Send prompt for duplicate account resolution.
     */
    private void sendDuplicateAccountPrompt(Player player, PlayerUserData userData) {
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text(messagesConfig.prefix())
                .append(Component.text("⚠ DUPLICATE ACCOUNT DETECTED")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD))
        );
        
        player.sendMessage(
            Component.text(messagesConfig.prefix())
                .append(Component.text(messagesConfig.duplicateAccount())
                    .color(NamedTextColor.YELLOW))
        );
        
        player.sendMessage(
            Component.text(messagesConfig.prefix())
                .append(Component.text("Use ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text("/account link")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.UNDERLINED))
                .append(Component.text(" to resolve your accounts.")
                    .color(NamedTextColor.GRAY))
        );
        
        player.sendMessage(Component.empty());
    }
    
    /**
     * Send suggestion to link account with email for web app access.
     */
    private void sendAccountLinkSuggestion(Player player) {
        player.sendMessage(Component.empty());
        
        player.sendMessage(
            Component.text(messagesConfig.prefix())
                .append(Component.text("💡 TIP: ")
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD))
                .append(Component.text("Link your account to access the web app!")
                    .color(NamedTextColor.GRAY))
        );
        
        player.sendMessage(
            Component.text(messagesConfig.prefix())
                .append(Component.text("Use ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text("/account create")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.UNDERLINED))
                .append(Component.text(" or ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text("/account link")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.UNDERLINED))
                .append(Component.text(" to get started.")
                    .color(NamedTextColor.GRAY))
        );
        
        player.sendMessage(Component.empty());
    }
}

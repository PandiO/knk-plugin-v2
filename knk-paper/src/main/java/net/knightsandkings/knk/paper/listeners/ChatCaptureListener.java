package net.knightsandkings.knk.paper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.knightsandkings.knk.paper.chat.ChatCaptureManager;

/**
 * Listener for player chat events.
 * Routes messages to the ChatCaptureManager if the player is in a capture session.
 * This ensures sensitive input (emails, passwords) is not broadcast to other players.
 */
public class ChatCaptureListener implements Listener {
    private final ChatCaptureManager captureManager;
    
    public ChatCaptureListener(ChatCaptureManager captureManager) {
        this.captureManager = captureManager;
    }
    
    /**
     * Handle player chat events.
     * If the player is capturing chat input, cancel the event and route to ChatCaptureManager.
     * Otherwise, let the message through normally.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (captureManager.isCapturingChat(event.getPlayer().getUniqueId())) {
            // Cancel the chat event so it's not broadcast
            event.setCancelled(true);
            
            // Process the message through the capture manager
            captureManager.handleChatInput(event.getPlayer(), event.getMessage());
        }
    }
}

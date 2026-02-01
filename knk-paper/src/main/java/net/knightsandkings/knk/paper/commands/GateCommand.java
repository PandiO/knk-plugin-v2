package net.knightsandkings.knk.paper.commands;

import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gate command implementation providing player and admin gate control.
 * Supports opening/closing, status, listing, and admin operations.
 */
public class GateCommand {
    private final GateManager gateManager;

    public GateCommand(GateManager gateManager) {
        this.gateManager = gateManager;
    }

    /**
     * Handle /gate open <name>
     */
    public boolean executeOpen(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate open <name>");
            return true;
        }

        String gateName = String.join(" ", args);
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        // Check permission
        if (!checkPermission(sender, "knk.gate.open." + gate.getId()) && 
            !checkPermission(sender, "knk.gate.open.*")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to open this gate.");
            return true;
        }

        // Check if gate is active
        if (!gate.isActive()) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' is not active.");
            return true;
        }

        // Check if gate is destroyed
        if (gate.isDestroyed()) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' is destroyed and cannot be opened.");
            return true;
        }

        // Try to open
        if (gateManager.openGate(gate.getId())) {
            sender.sendMessage(ChatColor.GREEN + "Opening gate '" + gateName + "'...");
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Gate '" + gateName + "' is already open or opening.");
            return true;
        }
    }

    /**
     * Handle /gate close <name>
     */
    public boolean executeClose(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate close <name>");
            return true;
        }

        String gateName = String.join(" ", args);
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        // Check permission
        if (!checkPermission(sender, "knk.gate.close." + gate.getId()) && 
            !checkPermission(sender, "knk.gate.close.*")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to close this gate.");
            return true;
        }

        // Check if gate is active
        if (!gate.isActive()) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' is not active.");
            return true;
        }

        // Try to close
        if (gateManager.closeGate(gate.getId())) {
            sender.sendMessage(ChatColor.GREEN + "Closing gate '" + gateName + "'...");
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Gate '" + gateName + "' is already closed or closing.");
            return true;
        }
    }

    /**
     * Handle /gate info <name>
     */
    public boolean executeInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate info <name>");
            return true;
        }

        String gateName = String.join(" ", args);
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        // Display gate information
        sender.sendMessage(ChatColor.GOLD + "━━━ Gate Info: " + gate.getName() + " ━━━");
        sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + gate.getId());
        sender.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.WHITE + gate.getGateType());
        sender.sendMessage(ChatColor.GRAY + "State: " + formatState(gate.getCurrentState()));
        sender.sendMessage(ChatColor.GRAY + "Active: " + ChatColor.WHITE + (gate.isActive() ? "✓" : "✗"));
        sender.sendMessage(ChatColor.GRAY + "Destroyed: " + ChatColor.WHITE + (gate.isDestroyed() ? "✓" : "✗"));
        sender.sendMessage(ChatColor.GRAY + "Health: " + ChatColor.WHITE + 
                String.format("%.0f/%.0f", gate.getHealthCurrent(), gate.getHealthMax()));
        sender.sendMessage(ChatColor.GRAY + "Invincible: " + ChatColor.WHITE + (gate.isInvincible() ? "✓" : "✗"));
        sender.sendMessage(ChatColor.GRAY + "Blocks: " + ChatColor.WHITE + gate.getBlocks().size());
        sender.sendMessage(ChatColor.GRAY + "Motion Type: " + ChatColor.WHITE + gate.getMotionType());
        sender.sendMessage(ChatColor.GRAY + "Face Direction: " + ChatColor.WHITE + gate.getFaceDirection());

        return true;
    }

    /**
     * Handle /gate list
     */
    public boolean executeList(CommandSender sender, String[] args) {
        final Location senderLoc;
        if (sender instanceof Player) {
            senderLoc = ((Player) sender).getLocation();
        } else {
            senderLoc = null;
        }

        List<CachedGate> gates = gateManager.getAllGates().values().stream()
            .filter(gate -> senderLoc == null || gate.getAnchorPoint().distance(senderLoc.toVector()) <= 50)
            .collect(Collectors.toList());

        if (gates.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No gates nearby.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "━━━ Nearby Gates ━━━");
        for (CachedGate gate : gates) {
            String statusColor = gate.getCurrentState() == AnimationState.OPEN ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            String distanceStr = senderLoc != null ? 
                String.format(" (%.0fm)", gate.getAnchorPoint().distance(senderLoc.toVector())) : "";
            
            sender.sendMessage(ChatColor.AQUA + "• " + gate.getName() + 
                    ChatColor.GRAY + " [" + gate.getGateType() + "]" +
                    statusColor + " " + gate.getCurrentState() + distanceStr);
        }

        return true;
    }

    /**
     * Handle /gate admin reload
     */
    public boolean executeAdminReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knk.gate.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading gates from API...");
        gateManager.reloadGates().thenRun(() -> {
            int gateCount = gateManager.getAllGates().size();
            sender.sendMessage(ChatColor.GREEN + "Loaded " + gateCount + " gates from API.");
        }).exceptionally(ex -> {
            sender.sendMessage(ChatColor.RED + "Failed to reload gates: " + ex.getMessage());
            return null;
        });

        return true;
    }

    /**
     * Handle /gate admin health <name> <amount>
     */
    public boolean executeAdminHealth(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knk.gate.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate admin health <name> <amount>");
            return true;
        }

        String gateName = args[0];
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            gate.setHealthCurrent(Math.max(0, Math.min(amount, gate.getHealthMax())));
            sender.sendMessage(ChatColor.GREEN + "Set gate health to " + gate.getHealthCurrent());
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid health value: " + args[1]);
            return true;
        }
    }

    /**
     * Handle /gate admin repair <name>
     */
    public boolean executeAdminRepair(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knk.gate.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate admin repair <name>");
            return true;
        }

        String gateName = args[0];
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        gate.setHealthCurrent(gate.getHealthMax());
        gate.setIsDestroyed(false);
        sender.sendMessage(ChatColor.GREEN + "Repaired gate '" + gateName + "'. Health: " + 
                gate.getHealthCurrent() + "/" + gate.getHealthMax());

        return true;
    }

    /**
     * Handle /gate admin tp <name>
     */
    public boolean executeAdminTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knk.gate.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can teleport.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /gate admin tp <name>");
            return true;
        }

        String gateName = args[0];
        CachedGate gate = gateManager.getGateByName(gateName);

        if (gate == null) {
            sender.sendMessage(ChatColor.RED + "Gate '" + gateName + "' not found.");
            return true;
        }

        Player player = (Player) sender;
        Vector anchorPoint = gate.getAnchorPoint();
        Location teleportLoc = new Location(player.getWorld(), 
            anchorPoint.getX() + 0.5, 
            anchorPoint.getY() + 1, 
            anchorPoint.getZ() + 0.5);

        player.teleport(teleportLoc);
        sender.sendMessage(ChatColor.GREEN + "Teleported to gate '" + gateName + "'.");

        return true;
    }

    /**
     * Check if sender has a permission.
     */
    private boolean checkPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Format animation state for display.
     */
    private String formatState(AnimationState state) {
        if (state == null) {
            return ChatColor.GRAY + "UNKNOWN";
        }
        return switch (state) {
            case OPEN -> ChatColor.GREEN + "OPEN";
            case OPENING -> ChatColor.YELLOW + "OPENING";
            case CLOSED -> ChatColor.RED + "CLOSED";
            case CLOSING -> ChatColor.YELLOW + "CLOSING";
            default -> ChatColor.GRAY + state.toString();
        };
    }
}

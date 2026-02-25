package net.knightsandkings.knk.paper.enchantment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FrozenPlayerTracker {
    private final Plugin plugin;
    private final Map<UUID, Integer> pendingUnfreezeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> frozenUntilMillis = new ConcurrentHashMap<>();

    public FrozenPlayerTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void freeze(Player player, int durationTicks) {
        if (player == null || durationTicks <= 0) {
            return;
        }

        UUID playerId = player.getUniqueId();
        frozenUntilMillis.put(playerId, System.currentTimeMillis() + (durationTicks * 50L));

        Integer previousTask = pendingUnfreezeTasks.remove(playerId);
        if (previousTask != null) {
            Bukkit.getScheduler().cancelTask(previousTask);
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            frozenUntilMillis.remove(playerId);
            pendingUnfreezeTasks.remove(playerId);
        }, durationTicks);
        pendingUnfreezeTasks.put(playerId, taskId);
    }

    public boolean isFrozen(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        Long frozenUntil = frozenUntilMillis.get(playerId);
        if (frozenUntil == null) {
            return false;
        }

        if (frozenUntil <= System.currentTimeMillis()) {
            frozenUntilMillis.remove(playerId);
            pendingUnfreezeTasks.remove(playerId);
            return false;
        }

        return true;
    }

    public void unfreeze(UUID playerId) {
        if (playerId == null) {
            return;
        }

        frozenUntilMillis.remove(playerId);
        Integer taskId = pendingUnfreezeTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

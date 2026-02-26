package net.knightsandkings.knk.paper.enchantment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FrozenPlayerTracker {
    private final Plugin plugin;
    private final Map<UUID, Integer> pendingUnfreezeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> frozenUntilMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> originalAiStates = new ConcurrentHashMap<>();

    public FrozenPlayerTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void freeze(LivingEntity target, int durationTicks) {
        if (target == null || durationTicks <= 0) {
            return;
        }

        UUID targetId = target.getUniqueId();
        frozenUntilMillis.put(targetId, System.currentTimeMillis() + (durationTicks * 50L));

        Integer previousTask = pendingUnfreezeTasks.remove(targetId);
        if (previousTask != null) {
            Bukkit.getScheduler().cancelTask(previousTask);
        }

        if (!(target instanceof Player)) {
            try {
                originalAiStates.put(targetId, target.hasAI());
                target.setAI(false);
            } catch (UnsupportedOperationException ignored) {
                originalAiStates.remove(targetId);
            }
            target.setVelocity(new Vector(0.0d, 0.0d, 0.0d));
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            restoreEntityState(targetId);
            frozenUntilMillis.remove(targetId);
            pendingUnfreezeTasks.remove(targetId);
        }, durationTicks);
        pendingUnfreezeTasks.put(targetId, taskId);
    }

    public void freeze(Player player, int durationTicks) {
        freeze((LivingEntity) player, durationTicks);
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
        restoreEntityState(playerId);
        Integer taskId = pendingUnfreezeTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void restoreEntityState(UUID entityId) {
        Boolean originalAi = originalAiStates.remove(entityId);
        if (originalAi == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof LivingEntity livingEntity && !(livingEntity instanceof Player)) {
            try {
                livingEntity.setAI(originalAi);
            } catch (UnsupportedOperationException ignored) {
                // No AI support on this entity implementation
            }
        }
    }
}

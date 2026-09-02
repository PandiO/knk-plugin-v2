package net.knightsandkings.knk.paper.tasks;

import net.knightsandkings.knk.api.dto.WorldTaskDto;
import net.knightsandkings.knk.core.ports.api.WorldTasksApi;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Periodically polls the Web API for Pending WorldTasks that don't require a player
 * and dispatches each to the first registered handler that supports its TaskType.
 * Runs no scanning/game logic itself; each handler owns its own thread-safety.
 */
public class HeadlessWorldTaskPoller {
    private static final Logger LOGGER = Logger.getLogger(HeadlessWorldTaskPoller.class.getName());
    private static final long POLL_INTERVAL_TICKS = 40L; // 2 seconds

    private final WorldTasksApi worldTasksApi;
    private final Plugin plugin;
    private final List<IHeadlessWorldTaskHandler> handlers = new ArrayList<>();
    private final Set<Integer> inFlightTaskIds = ConcurrentHashMap.newKeySet();

    private org.bukkit.scheduler.BukkitTask task;

    public HeadlessWorldTaskPoller(WorldTasksApi worldTasksApi, Plugin plugin) {
        this.worldTasksApi = worldTasksApi;
        this.plugin = plugin;
    }

    public void registerHandler(IHeadlessWorldTaskHandler handler) {
        handlers.add(handler);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin, this::pollOnce, POLL_INTERVAL_TICKS, POLL_INTERVAL_TICKS
        );
        LOGGER.info("HeadlessWorldTaskPoller started (interval=" + POLL_INTERVAL_TICKS + " ticks)");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void pollOnce() {
        if (handlers.isEmpty()) {
            return;
        }

        worldTasksApi.listByStatus("Pending").thenAccept(tasks -> {
            for (WorldTaskDto candidate : tasks) {
                dispatchIfSupported(candidate);
            }
        }).exceptionally(ex -> {
            LOGGER.warning("HeadlessWorldTaskPoller failed to list pending tasks: " + ex.getMessage());
            return null;
        });
    }

    private void dispatchIfSupported(WorldTaskDto candidate) {
        if (candidate == null || candidate.id() == null) {
            return;
        }

        if (!inFlightTaskIds.add(candidate.id())) {
            return; // already being processed from a previous poll cycle
        }

        for (IHeadlessWorldTaskHandler handler : handlers) {
            if (handler.supports(candidate.taskType())) {
                LOGGER.info("Dispatching headless task " + candidate.id() + " (" + candidate.taskType() + ")");
                try {
                    handler.execute(candidate, () -> inFlightTaskIds.remove(candidate.id()));
                } catch (Exception e) {
                    LOGGER.warning("Headless handler threw for task " + candidate.id() + ": " + e.getMessage());
                    inFlightTaskIds.remove(candidate.id());
                }
                return;
            }
        }

        // No handler for this task type; don't keep tracking it as in-flight.
        inFlightTaskIds.remove(candidate.id());
    }
}

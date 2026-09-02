package net.knightsandkings.knk.paper.tasks;

import net.knightsandkings.knk.api.dto.WorldTaskDto;

/**
 * Handler for WorldTasks that execute without a player (webapp-initiated).
 * Unlike {@link IWorldTaskHandler}, no claim phase is required: the poller
 * dispatches Pending tasks directly and the handler completes/fails them via the API.
 */
public interface IHeadlessWorldTaskHandler {
    /**
     * @param taskType the WorldTask.TaskType value
     * @return true if this handler processes tasks of this type
     */
    boolean supports(String taskType);

    /**
     * Execute the task. Implementations must call the task's complete/fail API
     * exactly once and then invoke onFinished, regardless of outcome.
     *
     * @param task the pending task to execute
     * @param onFinished callback invoked once the task has been completed or failed
     */
    void execute(WorldTaskDto task, Runnable onFinished);
}

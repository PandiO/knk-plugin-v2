package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.api.dto.WorldTaskDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API for managing WorldTasks - tasks that require in-game world data.
 */
public interface WorldTasksApi {
    /**
     * List tasks by status.
     * @param status Task status (e.g., "Pending", "Claimed", "Completed", "Failed")
     * @return List of tasks with the given status
     */
    CompletableFuture<List<WorldTaskDto>> listByStatus(String status);

    /**
     * Get a task by its link code.
     * @param linkCode The unique 6-character link code
     * @return The task, or null if not found
     */
    CompletableFuture<WorldTaskDto> getByLinkCode(String linkCode);

    /**
     * Get a task by its ID.
     * @param id The task ID
     * @return The task, or null if not found
     */
    CompletableFuture<WorldTaskDto> getById(int id);

    /**
     * Claim a task for processing.
     * @param id Task ID
     * @param linkCode The link code (for verification)
     * @param serverId Server ID claiming the task
     * @param minecraftUsername Username claiming the task
     * @return The updated task
     */
    CompletableFuture<WorldTaskDto> claim(int id, String linkCode, String serverId, String minecraftUsername);

    /**
     * Complete a task with output data.
     * @param id Task ID
     * @param outputJson JSON output data collected in-game
     * @return The completed task
     */
    CompletableFuture<WorldTaskDto> complete(int id, String outputJson);

    /**
     * Mark a task as failed.
     * @param id Task ID
     * @param errorMessage Error description
     * @return The failed task
     */
    CompletableFuture<WorldTaskDto> fail(int id, String errorMessage);
}

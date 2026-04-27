package il.openu.taskflow.repository;

import il.openu.taskflow.entity.Task;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 * Repository for Task entity - main operations for the Kanban board.
 */
@Stateless
public class TaskRepository extends AbstractRepository<Task> {

    public TaskRepository() {
        super(Task.class);
    }

    /**
     * Returns all tasks that belong to a specific board (used in Kanban view).
     * @param boardId ID of the board
     * @return list of tasks in that board
     */
    public List<Task> findByBoardId(Long boardId) {
        return em.createQuery("SELECT t FROM Task t WHERE t.board.id = :boardId ORDER BY t.id", Task.class)
                .setParameter("boardId", boardId)
                .getResultList();
    }


    /**
     * Returns all tasks for a specific board filtered by their status.
     * @param boardId ID of the board
     * @param status The status of the tasks to retrieve
     * @return list of tasks matching the board and status, ordered by last update
     */
    public List<Task> findByBoardIdAndStatus(Long boardId, Task.TaskStatus status) {
        return em.createQuery(
                        "SELECT t FROM Task t WHERE t.board.id = :boardId AND t.status = :status ORDER BY t.updatedAt DESC",
                        Task.class)
                .setParameter("boardId", boardId)
                .setParameter("status", status)
                .getResultList();
    }
}
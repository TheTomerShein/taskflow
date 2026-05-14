package il.openu.taskflow.repository;

import il.openu.taskflow.entity.ActivityLog;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 * Repository for ActivityLog entity - used for the activity feed.
 */
@Stateless
public class ActivityLogRepository extends AbstractRepository<ActivityLog> {

    public ActivityLogRepository() {
        super(ActivityLog.class);
    }

    /**
     * Returns recent activity for a specific project (used in activity feed).
     * @param projectId ID of the project
     * @return list of activity logs ordered by time (newest first)
     */
    public List<ActivityLog> findByProjectId(Long projectId) {
        return em.createQuery("SELECT a FROM ActivityLog a WHERE a.project.id = :projectId ORDER BY a.createdAt DESC", ActivityLog.class)
                .setParameter("projectId", projectId)
                .getResultList();
    }

    /**
     * Returns recent activity for a specific board.
     * @param boardId ID of the board
     * @return list of activity logs ordered by time (newest first)
     */
    public List<ActivityLog> findByBoardId(Long boardId) {
        return em.createQuery("SELECT a FROM ActivityLog a WHERE a.board.id = :boardId ORDER BY a.createdAt DESC", ActivityLog.class)
                .setParameter("boardId", boardId)
                .getResultList();
    }
}
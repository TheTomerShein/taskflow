package il.openu.taskflow.repository;

import il.openu.taskflow.entity.Comment;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 * Repository for Comment entity.
 */
@Stateless
public class CommentRepository extends AbstractRepository<Comment> {

    public CommentRepository() {
        super(Comment.class);
    }

    /**
     * Returns all comments for a specific task (used in task details modal).
     * @param taskId ID of the task
     * @return list of comments ordered by creation time
     */
    public List<Comment> findByTaskId(Long taskId) {
        return em.createQuery("SELECT c FROM Comment c WHERE c.task.id = :taskId ORDER BY c.createdAt DESC", Comment.class)
                .setParameter("taskId", taskId)
                .getResultList();
    }
}
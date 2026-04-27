package il.openu.taskflow.repository;

import il.openu.taskflow.entity.Board;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 * Repository for Board entity.
 */
@Stateless
public class BoardRepository extends AbstractRepository<Board> {

    public BoardRepository() {
        super(Board.class);
    }

    /**
     * Finds all boards that belong to a specific project.
     * @param projectId ID of the project
     * @return list of boards
     */
    public List<Board> findByProjectId(Long projectId) {
        return em.createQuery("SELECT b FROM Board b WHERE b.project.id = :projectId", Board.class)
                .setParameter("projectId", projectId)
                .getResultList();
    }
}
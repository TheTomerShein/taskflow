package il.openu.taskflow.repository;

import il.openu.taskflow.entity.Project;
import jakarta.ejb.Stateless;

import java.util.List;

/**
 * Repository for Project entity.
 */
@Stateless
public class ProjectRepository extends AbstractRepository<Project> {

    public ProjectRepository() {
        super(Project.class);
    }

    /**
     * Finds all projects that a specific user is a member of.
     *
     * @param userId the ID of the user
     * @return list of projects the user belongs to
     */
    public List<Project> findByMember(Long userId) {
        return em.createQuery(
                        "SELECT DISTINCT p FROM Project p JOIN p.members m WHERE m.id = :userId ORDER BY p.createdAt DESC",
                        Project.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
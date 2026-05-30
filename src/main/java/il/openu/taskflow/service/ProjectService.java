package il.openu.taskflow.service;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ProjectRepository;
import il.openu.taskflow.repository.UserRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for projects (create, add member, etc.) with async activity logging via JTA-synchronized CDI events.
 */
@Stateless
public class ProjectService {

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JmsProducer jmsProducer;

    /**
     * Creates a new project and sets the owner as the first member.
     * @param name project name
     * @param description project description
     * @param owner the user who creates the project
     * @return the created project
     */
    public Project createProject(String name, String description, User owner) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setOwner(owner);
        project.getMembers().add(owner);   // owner is also a member
        Project savedProject = projectRepository.save(project);

        // Send async JMS event
        jmsProducer.sendEvent(
                "PROJECT_CREATED",
                savedProject.getId(),
                null,
                null,
                owner.getId(),
                "פרויקט נוצר: " + name
        );

        return savedProject;
    }

    /**
     * Adds a user as a member to a project.
     * @param projectId project ID
     * @param userId user ID to add
     * @param addedBy the user who performed the action
     * @return true if added successfully
     */
    public boolean addMember(Long projectId, Long userId, User addedBy) {
        ProjectAndUser pu = fetchProjectAndUser(projectId, userId);
        if (pu == null) return false;

        // Avoid duplicates
        if (pu.project().getMembers().contains(pu.user())) {
            return false;
        }

        pu.project().getMembers().add(pu.user());
        projectRepository.update(pu.project());

        // Send async JMS event
        jmsProducer.sendEvent(
                "MEMBER_ADDED",
                pu.project().getId(),
                null,
                null,
                addedBy.getId(),
                "חבר נוסף: " + pu.user().getUsername()
        );

        return true;
    }

    /**
     * Returns all projects a user is member of (for dashboard).
     * @param userId user ID
     * @return list of projects
     */
    public List<Project> findByMember(Long userId) {
        return projectRepository.findByMember(userId);
    }

    /**
     * Find project by ID (needed for ProjectBean).
     */
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Removes a member from a project and logs the action asynchronously.
     * @param projectId project ID
     * @param userId user ID to remove
     * @param removedBy the user who performed the action
     * @return true if removed successfully
     */
    public boolean removeMember(Long projectId, Long userId, User removedBy) {
        ProjectAndUser pu = fetchProjectAndUser(projectId, userId);
        if (pu == null) return false;

        // Cannot remove the owner
        if (pu.project().isOwner(pu.user())) {
            return false;
        }

        if (!pu.project().getMembers().contains(pu.user())) {
            return false;
        }

        pu.project().getMembers().remove(pu.user());
        projectRepository.update(pu.project());

        // Send async JMS event
        jmsProducer.sendEvent(
                "MEMBER_REMOVED",
                pu.project().getId(),
                null,
                null,
                removedBy.getId(),
                "חבר הוסר: " + pu.user().getUsername()
        );

        return true;
    }

    /**
     * Updates the project name/description and logs the action asynchronously.
     * @param project the project to update
     * @param user the user who performed the action
     * @return the updated project
     */
    public Project updateProject(Project project, User user) {
        Project updated = projectRepository.update(project);

        // Send async JMS event
        jmsProducer.sendEvent(
                "PROJECT_UPDATED",
                updated.getId(),
                null,
                null,
                user.getId(),
                "פרויקט עודכן: " + project.getName()
        );

        return updated;
    }

    /**
     * Checks if a user is the owner of a project.
     * @param projectId project ID
     * @param userId user ID
     * @return true if the user is the owner
     */
    public boolean isOwner(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .map(project -> project.isOwner(
                        userRepository.findById(userId).orElse(null)))
                .orElse(false);
    }

    // ==================== Private Helpers ====================

    /**
     * Lightweight holder for a fetched Project and User pair.
     * Useful for operations that require both entities to be validated and loaded.
     */
    private static class ProjectAndUser {
        private final Project project;
        private final User user;

        ProjectAndUser(Project project, User user) {
            this.project = project;
            this.user = user;
        }

        Project project() { return project; }
        User user() { return user; }
    }

    /**
     * Fetches a Project and User by their IDs.
     * Returns null if either entity is not found in the database.
     * 
     * @param projectId the project ID
     * @param userId the user ID
     * @return a ProjectAndUser containing both entities, or null if missing
     */
    private ProjectAndUser fetchProjectAndUser(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (project == null || user == null) {
            return null;
        }
        return new ProjectAndUser(project, user);
    }
}
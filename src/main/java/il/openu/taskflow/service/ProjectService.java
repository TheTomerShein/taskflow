package il.openu.taskflow.service;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ProjectRepository;
import il.openu.taskflow.repository.UserRepository;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
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
    private Event<ActivityEvent> eventPublisher;

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

        // Fire CDI event — will be processed after transaction commits successfully
        eventPublisher.fire(new ActivityEvent(
                "PROJECT_CREATED",
                savedProject.getId(),
                null,
                null,
                owner.getId(),
                "Project created: " + name
        ));

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
        Project project = projectRepository.findById(projectId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (project == null || user == null) {
            return false;
        }

        // Avoid duplicates
        if (project.getMembers().contains(user)) {
            return false;
        }

        project.getMembers().add(user);
        projectRepository.update(project);

        // Fire CDI event — will be processed after transaction commits successfully
        eventPublisher.fire(new ActivityEvent(
                "MEMBER_ADDED",
                project.getId(),
                null,
                null,
                addedBy.getId(),
                "Member added: " + user.getUsername()
        ));

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
        Project project = projectRepository.findById(projectId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (project == null || user == null) {
            return false;
        }

        // Cannot remove the owner
        if (project.getOwner().getId().equals(userId)) {
            return false;
        }

        if (!project.getMembers().contains(user)) {
            return false;
        }

        project.getMembers().remove(user);
        projectRepository.update(project);

        // Fire CDI event — will be processed after transaction commits successfully
        eventPublisher.fire(new ActivityEvent(
                "MEMBER_REMOVED",
                project.getId(),
                null,
                null,
                removedBy.getId(),
                "Member removed: " + user.getUsername()
        ));

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

        // Fire CDI event — will be processed after transaction commits successfully
        eventPublisher.fire(new ActivityEvent(
                "PROJECT_UPDATED",
                updated.getId(),
                null,
                null,
                user.getId(),
                "Project updated: " + project.getName()
        ));

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
                .map(project -> project.getOwner().getId().equals(userId))
                .orElse(false);
    }
}
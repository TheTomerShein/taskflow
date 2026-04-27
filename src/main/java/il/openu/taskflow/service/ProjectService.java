package il.openu.taskflow.service;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ProjectRepository;
import il.openu.taskflow.repository.UserRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Business logic for projects (create, add member, etc.)
 */
@Stateless
public class ProjectService {

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private UserRepository userRepository;

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
        return projectRepository.save(project);
    }

    /**
     * Adds a user as a member to a project.
     * @param projectId project ID
     * @param userId user ID to add
     * @return true if added successfully
     */
    public boolean addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId);
        User user = userRepository.findById(userId);

        if (project == null || user == null) {
            return false;
        }

        // Avoid duplicates
        if (project.getMembers().contains(user)) {
            return false;
        }

        project.getMembers().add(user);
        projectRepository.update(project);
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
    public Project findById(Long id) {
        return projectRepository.findById(id);
    }
}
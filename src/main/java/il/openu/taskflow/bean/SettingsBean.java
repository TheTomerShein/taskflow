package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.UserRepository;
import il.openu.taskflow.service.ProjectService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Backing bean for the Project Settings page.
 * Handles project editing, member search, add and remove.
 * Only accessible by the project owner.
 */
@Named
@ViewScoped
public class SettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProjectService projectService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthBean authBean;

    private Long projectId;
    private Project currentProject;
    private List<User> members;

    // For adding members
    private User selectedUser;

    /**
     * Called by f:viewParam. Loads the project and verifies ownership.
     */
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        loadProject();
    }

    public Long getProjectId() {
        return projectId;
    }

    /**
     * Loads the project and its members. Redirects if not the owner.
     */
    private void loadProject() {
        if (projectId == null) return;

        currentProject = projectService.findById(projectId).orElse(null);

        if (currentProject == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Project not found"));
            return;
        }

        // Security: only owner can access settings
        User currentUser = authBean.getCurrentUser();
        if (currentUser == null || !currentProject.getOwner().getId().equals(currentUser.getId())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Only the project owner can access settings"));
            currentProject = null;
            return;
        }

        members = new ArrayList<>(currentProject.getMembers());
    }

    /**
     * Autocomplete: searches users by username/email, filtering out existing members.
     */
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<User> results = userRepository.searchByTerm(query.trim());

        // Filter out users who are already members
        if (currentProject != null) {
            List<Long> memberIds = currentProject.getMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            results = results.stream()
                    .filter(u -> !memberIds.contains(u.getId()))
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Adds the selected user as a member to the project.
     */
    public void addMember() {
        if (selectedUser == null || currentProject == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Please select a user first"));
            return;
        }

        boolean success = projectService.addMember(projectId, selectedUser.getId(), authBean.getCurrentUser());

        if (success) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                            "Member '" + selectedUser.getUsername() + "' added successfully"));
            selectedUser = null;
            // Reload project to refresh members list
            currentProject = projectService.findById(projectId).orElse(null);
            members = new ArrayList<>(currentProject.getMembers());
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "User is already a member or not found"));
        }
    }

    /**
     * Removes a member from the project.
     */
    public void removeMember(Long userId) {
        if (currentProject == null) return;

        boolean success = projectService.removeMember(projectId, userId, authBean.getCurrentUser());

        if (success) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Member removed successfully"));
            // Reload
            currentProject = projectService.findById(projectId).orElse(null);
            members = new ArrayList<>(currentProject.getMembers());
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot remove this member (owner cannot be removed)"));
        }
    }

    /**
     * Saves the project name and description.
     */
    public void saveProject() {
        if (currentProject == null) return;

        projectService.updateProject(currentProject, authBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Project details updated successfully"));
    }

    // Getters & Setters
    public Project getCurrentProject() { return currentProject; }
    public List<User> getMembers() { return members; }
    public User getSelectedUser() { return selectedUser; }
    public void setSelectedUser(User selectedUser) { this.selectedUser = selectedUser; }
}

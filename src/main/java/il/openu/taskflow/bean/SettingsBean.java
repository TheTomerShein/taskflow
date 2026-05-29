package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.UserRepository;
import il.openu.taskflow.service.ProjectService;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;


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
public class SettingsBean extends BaseBean {

    @Inject
    private ProjectService projectService;

    @Inject
    private UserRepository userRepository;



    private Long projectId;
    private Project currentProject;
    private List<User> members;

    // For adding members
    private List<User> selectedUsers = new ArrayList<>();

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
            addErrorMessage("שגיאה", "הפרויקט לא נמצא");
            return;
        }

        // Security: only owner can access settings
        User currentUser = getAuthBean().getCurrentUser();
        if (currentUser == null || !currentProject.isOwner(currentUser)) {
            addErrorMessage("שגיאה", "רק בעל הפרויקט יכול לגשת להגדרות");
            currentProject = null;
            return;
        }

        reloadMembers();
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
     * Adds the selected users as members to the project.
     */
    public void addMember() {
        if (selectedUsers == null || selectedUsers.isEmpty() || currentProject == null) {
            addWarnMessage("אזהרה", "אנא בחר לפחות משתמש אחד תחילה");
            return;
        }

        int addedCount = 0;
        List<String> skippedUsernames = new ArrayList<>();

        for (User user : selectedUsers) {
            boolean success = projectService.addMember(projectId, user.getId(), getAuthBean().getCurrentUser());
            if (success) {
                addedCount++;
            } else {
                skippedUsernames.add(user.getUsername());
            }
        }

        if (addedCount > 0) {
            if (skippedUsernames.isEmpty()) {
                addInfoMessage("הצלחה", "נוספו " + addedCount + " חברי צוות בהצלחה");
            } else {
                addInfoMessage("הצלחה", "נוספו " + addedCount + " חברי צוות. לא ניתן להוסיף את: " + String.join(", ", skippedUsernames));
            }
            selectedUsers.clear();
            // Reload project to refresh members list
            reloadMembers();
        } else {
            addWarnMessage("אזהרה", "המשתמשים שבחרת כבר חברים בפרויקט או שלא נמצאו");
        }
    }

    /**
     * Removes a member from the project.
     */
    public void removeMember(Long userId) {
        if (currentProject == null) return;

        boolean success = projectService.removeMember(projectId, userId, getAuthBean().getCurrentUser());

        if (success) {
            addInfoMessage("הצלחה", "חבר הצוות הוסר בהצלחה");
            // Reload
            reloadMembers();
        } else {
            addErrorMessage("שגיאה", "לא ניתן להסיר חבר זה (לא ניתן להסיר את הבעלים)");
        }
    }

    /**
     * Saves the project name and description.
     */
    public void saveProject() {
        if (currentProject == null) return;

        projectService.updateProject(currentProject, getAuthBean().getCurrentUser());

        addInfoMessage("הצלחה", "פרטי הפרויקט עודכנו בהצלחה");
    }

    /**
     * Reloads the project from the database and refreshes the members list.
     */
    private void reloadMembers() {
        currentProject = projectService.findById(projectId).orElse(null);
        members = (currentProject != null) ? new ArrayList<>(currentProject.getMembers()) : new ArrayList<>();
    }

    // Getters & Setters
    public Project getCurrentProject() { return currentProject; }
    public List<User> getMembers() { return members; }
    public List<User> getSelectedUsers() { return selectedUsers; }
    public void setSelectedUsers(List<User> selectedUsers) { this.selectedUsers = selectedUsers; }
}

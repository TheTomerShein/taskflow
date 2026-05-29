package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.service.ProjectService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;



@Named
@ViewScoped
public class DashboardBean extends BaseBean {

    @Inject
    private ProjectService projectService;


    private List<Project> projects;
    private String newProjectName;
    private String newProjectDescription;


    @PostConstruct
    public void init() {
        if (getAuthBean().getCurrentUser() != null) {
            this.projects = projectService.findByMember(getAuthBean().getCurrentUser().getId());
        }
    }

    public String createProject() {
        User currentUser = getAuthenticatedUser();
        if (currentUser == null) return null;

        Project project = projectService.createProject(newProjectName, newProjectDescription, currentUser);
        if (project != null) {
            addInfoMessage("הצלחה", "הפרויקט '" + newProjectName + "' נוצר בהצלחה");
            // Refresh list
            this.projects = projectService.findByMember(currentUser.getId());
            newProjectName = "";
            newProjectDescription = "";
        }

        return null; // stay on same page
    }

    // Getters & Setters
    public List<Project> getProjects() {
        return projects;
    }

    public String getNewProjectName() {
        return newProjectName;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }

    public String getNewProjectDescription() {
        return newProjectDescription;
    }

    public void setNewProjectDescription(String newProjectDescription) {
        this.newProjectDescription = newProjectDescription;
    }
}
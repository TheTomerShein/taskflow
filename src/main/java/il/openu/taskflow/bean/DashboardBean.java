package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.service.ProjectService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

import java.util.logging.Logger;

@Named
@ViewScoped
public class DashboardBean implements Serializable {

    @Inject
    private ProjectService projectService;

    @Inject
    private AuthBean authBean;

    private List<Project> projects;
    private String newProjectName;
    private String newProjectDescription;
    private static final Logger LOGGER = Logger.getLogger(DashboardBean.class.getName());

    @PostConstruct
    public void init() {
        if (authBean.getCurrentUser() != null) {
            this.projects = projectService.findByMember(authBean.getCurrentUser().getId());
        }
    }

    public String createProject() {
        User currentUser = authBean.getCurrentUser();
        if (currentUser == null) return "login?faces-redirect=true";

        Project project = projectService.createProject(newProjectName, newProjectDescription, currentUser);
        if (project != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Project created", newProjectName));
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
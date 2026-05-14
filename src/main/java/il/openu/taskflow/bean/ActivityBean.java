package il.openu.taskflow.bean;

import il.openu.taskflow.entity.ActivityLog;
import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.repository.ActivityLogRepository;
import il.openu.taskflow.repository.BoardRepository;
import il.openu.taskflow.service.ProjectService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class ActivityBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ActivityLogRepository activityLogRepository;

    @Inject
    private ProjectService projectService;

    @Inject
    private BoardRepository boardRepository;

    private Long projectId;
    private Long boardId;
    
    private Project currentProject;
    private Board currentBoard;
    private List<ActivityLog> activityLogs;

    @PostConstruct
    public void init() {
        // Initialized via viewParam setters
    }

    public void loadLogs() {
        if (boardId != null && boardId > 0) {
            currentBoard = boardRepository.findById(boardId).orElse(null);
            if (currentBoard != null) {
                currentProject = currentBoard.getProject();
                activityLogs = activityLogRepository.findByBoardId(boardId);
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Board not found"));
            }
        } else if (projectId != null && projectId > 0) {
            currentProject = projectService.findById(projectId).orElse(null);
            if (currentProject != null) {
                activityLogs = activityLogRepository.findByProjectId(projectId);
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Project not found"));
            }
        }
    }

    public void refreshLogs() {
        if (boardId != null && boardId > 0) {
            activityLogs = activityLogRepository.findByBoardId(boardId);
        } else if (projectId != null && projectId > 0) {
            activityLogs = activityLogRepository.findByProjectId(projectId);
        }
    }

    public String goBack() {
        if (boardId != null && boardId > 0) {
            return "kanban?faces-redirect=true&boardId=" + boardId;
        } else if (projectId != null && projectId > 0) {
            return "project?faces-redirect=true&projectId=" + projectId;
        }
        return "dashboard?faces-redirect=true";
    }

    // Getters and Setters
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        // Only load if boardId isn't taking precedence
        if (this.boardId == null) {
            loadLogs();
        }
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
        loadLogs();
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public Board getCurrentBoard() {
        return currentBoard;
    }

    public List<ActivityLog> getActivityLogs() {
        return activityLogs;
    }
}

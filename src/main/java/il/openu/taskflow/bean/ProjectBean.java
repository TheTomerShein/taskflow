package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.service.ProjectService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

@Named
@RequestScoped
public class ProjectBean {

    @Inject
    private ProjectService projectService;
    @Inject
    private BoardService boardService;
    @Inject
    private AuthBean authBean;

    private Long projectId;
    private Project currentProject;
    private List<Board> boards;

    // ליצירת Board חדש
    private String newBoardName;

    public void loadProject() {
        if (projectId != null && authBean.getCurrentUser() != null) {
            currentProject = projectService.findById(projectId);
            if (currentProject != null) {
                boards = boardService.findByProjectId(projectId);
            }
        }
    }

    public String createBoard() {
        if (newBoardName == null || newBoardName.trim().isEmpty() || currentProject == null) {
            return null;
        }

        Board board = boardService.createBoard(newBoardName.trim(), currentProject.getId(), authBean.getCurrentUser());
        if (board != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Board Created", newBoardName));
            boards = boardService.findByProjectId(currentProject.getId()); // רענון
            newBoardName = "";
        }
        return null;
    }

    /**
     * ניווט ל-Kanban של Board ספציפי (routing: /kanban?boardId=XX)
     */
    public String navigateToKanban(Long boardId) {
        return boardId != null ? "kanban?faces-redirect=true&boardId=" + boardId : null;
    }

    // Getters & Setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Project getCurrentProject() { return currentProject; }
    public List<Board> getBoards() { return boards; }

    public String getNewBoardName() { return newBoardName; }
    public void setNewBoardName(String newBoardName) { this.newBoardName = newBoardName; }
}
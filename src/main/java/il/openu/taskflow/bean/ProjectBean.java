package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.repository.BoardRepository;
import il.openu.taskflow.service.BoardService;
import il.openu.taskflow.service.ProjectService;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

@Named
@ViewScoped
public class ProjectBean extends BaseBean {

    @Inject
    private ProjectService projectService;

    @Inject
    private BoardRepository boardRepository;

    @Inject
    private BoardService boardService;


    private Long projectId;
    private Project currentProject;
    private List<Board> boards;

    private String newBoardName;
    private String newBoardDescription;



    public void loadProject() {
        if (projectId != null) {
            currentProject = projectService.findById(projectId).orElse(null);
            if (currentProject != null) {
                boards = boardRepository.findByProjectId(projectId);
            } else {
                addErrorMessage("שגיאה", "פרויקט לא נמצא");
            }
        }
    }

    public String createBoard() {
        if (currentProject == null || newBoardName == null || newBoardName.trim().isEmpty()) {
            addErrorMessage("שגיאה", "שם הלוח חובה");
            return null;
        }

        boardService.createBoard(newBoardName.trim(), currentProject, getAuthBean().getCurrentUser());

        addInfoMessage("לוח נוצר", "הלוח '" + newBoardName + "' נוצר בהצלחה");

        newBoardName = null;
        newBoardDescription = null;

        // Refresh list
        boards = boardRepository.findByProjectId(projectId);

        return null; // stay on page
    }

    // Getters & Setters
    public Long getProjectId() { return projectId; }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        loadProject();
    }

    public Project getCurrentProject() { return currentProject; }

    public List<Board> getBoards() { return boards; }

    public String getNewBoardName() { return newBoardName; }
    public void setNewBoardName(String newBoardName) { this.newBoardName = newBoardName; }

    public String getNewBoardDescription() { return newBoardDescription; }
    public void setNewBoardDescription(String newBoardDescription) { this.newBoardDescription = newBoardDescription; }

    /**
     * Returns true if the current logged-in user is the owner of this project.
     */
    public boolean isOwner() {
        if (currentProject == null || getAuthBean().getCurrentUser() == null) return false;
        return currentProject.isOwner(getAuthBean().getCurrentUser());
    }
}

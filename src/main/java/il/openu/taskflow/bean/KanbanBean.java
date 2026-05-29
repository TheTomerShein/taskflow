package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Task;
import il.openu.taskflow.repository.BoardRepository;
import il.openu.taskflow.service.TaskService;
import il.openu.taskflow.entity.Comment;
import il.openu.taskflow.repository.CommentRepository;
import il.openu.taskflow.repository.TaskRepository;
import il.openu.taskflow.repository.UserRepository;
import il.openu.taskflow.exception.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Named
@ViewScoped
public class KanbanBean extends BaseBean {


    @Inject
    private TaskService taskService;


    @Inject
    private BoardRepository boardRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private TaskRepository taskRepository;

    private List<Task> todoTasks = new ArrayList<>();
    private List<Task> inProgressTasks = new ArrayList<>();
    private List<Task> doneTasks = new ArrayList<>();

    private Long currentBoardId;
    private Board currentBoard;

    private String newTaskTitle;
    private String newTaskDescription;
    private Task.TaskStatus newTaskStatus = Task.TaskStatus.TODO;
    private LocalDateTime newTaskDueDate;
    private Long newTaskAssigneeId;

    private Task selectedTask;
    private Long selectedAssigneeId;
    private String newCommentText;
    private List<Comment> selectedTaskComments = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Note: currentBoardId is set by the f:viewParam setter AFTER PostConstruct.
        // The setter already calls loadTasks(), so we only need a fallback here
        // in case the setter fires before PostConstruct (rare edge case).
        if (currentBoardId != null) {
            loadTasks();
        }
    }

    /**
     * Loads all tasks for the current board grouped by status.
     * Called by the setCurrentBoardId() setter, which is invoked by f:viewParam.
     */
    public void loadTasks() {
        if (currentBoardId != null && currentBoardId > 0) {
            todoTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.TODO);
            inProgressTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.IN_PROGRESS);
            doneTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.DONE);

            if (currentBoard == null || !currentBoard.getId().equals(currentBoardId)) {
                currentBoard = boardRepository.findById(currentBoardId).orElse(null);
            }
        } else {
            todoTasks = List.of();
            inProgressTasks = List.of();
            doneTasks = List.of();
            currentBoard = null;
        }
    }

    public void createNewTask() {
        if (currentBoardId == null || currentBoardId <= 0 || newTaskTitle == null || newTaskTitle.trim().isEmpty()) {
            addErrorMessage("שגיאה", "כותרת המשימה נדרשת ויש לבחור לוח");
            return;
        }

        if (getAuthenticatedUser() == null) {
            return;
        }

        try {
            Task newTask = taskService.createTask(
                    newTaskTitle.trim(),
                    newTaskDescription != null ? newTaskDescription.trim() : "",
                    newTaskStatus != null ? newTaskStatus : Task.TaskStatus.TODO,
                    currentBoardId,
                    getAuthBean().getCurrentUser().getId(),
                    newTaskAssigneeId,
                    newTaskDueDate
            );

            addInfoMessage("משימה נוצרה", "המשימה '" + newTaskTitle + "' נוצרה בהצלחה");

            newTaskTitle = null;
            newTaskDescription = null;
            newTaskStatus = Task.TaskStatus.TODO;
            newTaskDueDate = null;
            newTaskAssigneeId = null;
            loadTasks();
        } catch (EntityNotFoundException | UnauthorizedException | IllegalArgumentException e) {
            addErrorMessage("שגיאה", "יצירת המשימה נכשלה: " + e.getMessage());
        }
    }

    public void onTaskDropFromJS() {
        if (getAuthenticatedUser() == null) {
            return;
        }

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        String taskIdStr = params.get("taskId");
        String newStatusStr = params.get("newStatus");

        if (taskIdStr == null || newStatusStr == null) {
            addWarnMessage("שגיאה", "נתוני גרירה לא תקינים");
            return;
        }

        try {
            Long taskId = Long.parseLong(taskIdStr);
            Task.TaskStatus newStatus = Task.TaskStatus.valueOf(newStatusStr);

            Task existingTask = findTaskById(taskId);
            if (existingTask == null) {
                 addErrorMessage("שגיאה", "המשימה לא נמצאה בתצוגה");
                 return;
            }

            if (newStatus.equals(existingTask.getStatus())) {
                return;
            }

            taskService.moveTask(taskId, newStatus, getAuthBean().getCurrentUser().getId());
            loadTasks();

            String hebrewStatus = newStatus == Task.TaskStatus.TODO ? "לביצוע" : (newStatus == Task.TaskStatus.IN_PROGRESS ? "בתהליך" : "הושלם");
            addInfoMessage("משימה הועברה", "המשימה '" + existingTask.getTitle() + "' הועברה ל-" + hebrewStatus);
        } catch (Exception e) {
            addErrorMessage("שגיאה", "העברת המשימה נכשלה: " + e.getMessage());
        }
    }

    private Task findTaskById(Long taskId) {
        return Stream.of(todoTasks, inProgressTasks, doneTasks)
                .flatMap(List::stream)
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }



    public void viewTask(Task task) {
        this.selectedTask = task;
        this.selectedAssigneeId = (task.getAssignee() != null) ? task.getAssignee().getId() : null;
        this.selectedTaskComments = commentRepository.findByTaskId(task.getId());
        this.newCommentText = "";
    }

    public List<il.openu.taskflow.entity.User> getProjectMembers() {
        if (currentBoard != null && currentBoard.getProject() != null) {
            return new ArrayList<>(currentBoard.getProject().getMembers());
        }
        return new ArrayList<>();
    }

    public void updateTask() {
        if (selectedTask == null) return;

        // Capture old assignee ID before making changes (for change detection)
        Long oldAssigneeId = (selectedTask.getAssignee() != null) ? selectedTask.getAssignee().getId() : null;

        if (selectedAssigneeId != null && selectedAssigneeId > 0) {
            il.openu.taskflow.entity.User assignee = userRepository.findById(selectedAssigneeId).orElse(null);
            selectedTask.setAssignee(assignee);
        } else {
            selectedTask.setAssignee(null);
        }

        try {
            taskService.updateTask(selectedTask, getAuthBean().getCurrentUser(), oldAssigneeId);
            addInfoMessage("הצלחה", "המשימה עודכנה בהצלחה");
            loadTasks();
        } catch (IllegalArgumentException e) {
            addErrorMessage("שגיאה", e.getMessage());
        }
    }

    public void addComment() {
        if (selectedTask == null || newCommentText == null || newCommentText.trim().isEmpty()) return;

        try {
            taskService.addComment(selectedTask.getId(), getAuthBean().getCurrentUser().getId(), newCommentText.trim());
            this.selectedTaskComments = commentRepository.findByTaskId(selectedTask.getId());
            this.newCommentText = "";
            addInfoMessage("הצלחה", "התגובה נוספה בהצלחה");
        } catch (Exception e) {
            addErrorMessage("שגיאה", "נכשל בהוספת התגובה");
        }
    }

    public String goToProject() {
        if (currentBoard != null && currentBoard.getProject() != null) {
            return "project?faces-redirect=true&projectId=" + currentBoard.getProject().getId();
        }
        return "dashboard?faces-redirect=true";
    }

    public void setNewTaskStatus(Task.TaskStatus status) {
        this.newTaskStatus = status;
    }

    // Getters and Setters
    public List<Task> getTodoTasks() {
        return todoTasks;
    }

    public List<Task> getInProgressTasks() {
        return inProgressTasks;
    }

    public List<Task> getDoneTasks() {
        return doneTasks;
    }

    public Long getCurrentBoardId() {
        return currentBoardId;
    }

    public void setCurrentBoardId(Long currentBoardId) {
        this.currentBoardId = currentBoardId;
        loadTasks();
    }

    public Board getCurrentBoard() {
        return currentBoard;
    }

    public String getCurrentBoardName() {
        return (currentBoard != null) ? currentBoard.getName() : "Board not selected";
    }

    public String getCurrentProjectName() {
        return (currentBoard != null && currentBoard.getProject() != null)
                ? currentBoard.getProject().getName() : "";
    }

    public String getNewTaskTitle() {
        return newTaskTitle;
    }

    public void setNewTaskTitle(String newTaskTitle) {
        this.newTaskTitle = newTaskTitle;
    }

    public String getNewTaskDescription() {
        return newTaskDescription;
    }

    public void setNewTaskDescription(String newTaskDescription) {
        this.newTaskDescription = newTaskDescription;
    }

    public LocalDateTime getNewTaskDueDate() {
        return newTaskDueDate;
    }

    public void setNewTaskDueDate(LocalDateTime newTaskDueDate) {
        this.newTaskDueDate = newTaskDueDate;
    }

    public LocalDateTime getMinDueDate() {
        return LocalDateTime.now();
    }

    public Task.TaskStatus getNewTaskStatus() {
        return newTaskStatus;
    }

    public Task.TaskStatus[] getAvailableStatuses() {
        return Task.TaskStatus.values();
    }

    public Task getSelectedTask() {
        return selectedTask;
    }

    public Long getSelectedAssigneeId() {
        return selectedAssigneeId;
    }

    public void setSelectedAssigneeId(Long selectedAssigneeId) {
        this.selectedAssigneeId = selectedAssigneeId;
    }

    public String getNewCommentText() {
        return newCommentText;
    }

    public void setNewCommentText(String newCommentText) {
        this.newCommentText = newCommentText;
    }

    public List<Comment> getSelectedTaskComments() {
        return selectedTaskComments;
    }

    public Long getNewTaskAssigneeId() {
        return newTaskAssigneeId;
    }

    public void setNewTaskAssigneeId(Long newTaskAssigneeId) {
        this.newTaskAssigneeId = newTaskAssigneeId;
    }
}

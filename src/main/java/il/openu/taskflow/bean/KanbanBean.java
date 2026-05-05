package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Task;
import il.openu.taskflow.repository.BoardRepository;
import il.openu.taskflow.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.event.DragDropEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Named
@ViewScoped
public class KanbanBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private TaskService taskService;

    @Inject
    private AuthBean authBean;

    @Inject
    private BoardRepository boardRepository;

    private List<Task> todoTasks = new ArrayList<>();
    private List<Task> inProgressTasks = new ArrayList<>();
    private List<Task> doneTasks = new ArrayList<>();

    private Long currentBoardId;
    private Board currentBoard;

    private String newTaskTitle;
    private String newTaskDescription;
    private Task.TaskStatus newTaskStatus = Task.TaskStatus.TODO;

    private Task selectedTask;

    @PostConstruct
    public void init() {
        // Load tasks if boardId was already set via viewParam before PostConstruct
        if (currentBoardId != null) {
            loadTasks();
        }
    }

    public void loadTasks() {
        if (currentBoardId != null && currentBoardId > 0) {
            todoTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.TODO);
            inProgressTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.IN_PROGRESS);
            doneTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.DONE);

            if (currentBoard == null || !currentBoard.getId().equals(currentBoardId)) {
                currentBoard = boardRepository.findById(currentBoardId);
            }
        } else {
            todoTasks = new ArrayList<>();
            inProgressTasks = new ArrayList<>();
            doneTasks = new ArrayList<>();
            currentBoard = null;
        }
    }

    public void createNewTask() {
        if (currentBoardId == null || currentBoardId <= 0 || newTaskTitle == null || newTaskTitle.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Task title is required and board must be selected"));
            return;
        }

        if (authBean.getCurrentUser() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not authenticated"));
            return;
        }

        Task newTask = taskService.createTask(
                newTaskTitle.trim(),
                newTaskDescription != null ? newTaskDescription.trim() : "",
                newTaskStatus != null ? newTaskStatus : Task.TaskStatus.TODO,
                currentBoardId,
                authBean.getCurrentUser().getId(),
                null
        );

        if (newTask != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Created",
                            "Task '" + newTaskTitle + "' created successfully"));

            newTaskTitle = null;
            newTaskDescription = null;
            newTaskStatus = Task.TaskStatus.TODO;
            loadTasks();
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "Failed to create task - make sure you are a member of the project"));
        }
    }

    public void onTaskDrop(DragDropEvent event) {
        String dragId = event.getDragId();
        String dropId = event.getDropId();

        if (dragId == null || dropId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "Invalid drag/drop data"));
            return;
        }

        if (authBean.getCurrentUser() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not authenticated"));
            return;
        }

        // Extract task ID from dragId (format: ...:taskCard_123 or just taskCard_123)
        Long taskId = extractTaskIdFromDragId(dragId);
        if (taskId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid task identifier"));
            return;
        }

        Task.TaskStatus newStatus = getStatusFromDropZone(dropId);
        if (newStatus == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "Invalid drop zone"));
            return;
        }

        // Find the task to get its title for message
        Task existingTask = findTaskById(taskId);
        if (existingTask == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Task not found"));
            return;
        }

        if (newStatus.equals(existingTask.getStatus())) {
            // Dropped on same column - no change needed
            return;
        }

        Task updatedTask = taskService.moveTask(taskId, newStatus, authBean.getCurrentUser().getId());

        if (updatedTask != null) {
            loadTasks();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Moved",
                            "Task '" + existingTask.getTitle() + "' moved to " + newStatus));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Move failed - insufficient permissions or task not found"));
        }
    }

    /**
     * Extracts task ID from a drag element client ID.
     * Handles formats like: kanbanForm:todoColumn:taskCard_123 or just taskCard_123
     */
    private Long extractTaskIdFromDragId(String dragId) {
        if (dragId == null) return null;
        Pattern pattern = Pattern.compile("taskCard_(\\d+)");
        Matcher matcher = pattern.matcher(dragId);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Task findTaskById(Long taskId) {
        for (Task task : todoTasks) {
            if (task.getId().equals(taskId)) return task;
        }
        for (Task task : inProgressTasks) {
            if (task.getId().equals(taskId)) return task;
        }
        for (Task task : doneTasks) {
            if (task.getId().equals(taskId)) return task;
        }
        return null;
    }

    private Task.TaskStatus getStatusFromDropZone(String dropId) {
        if (dropId == null) return null;
        if (dropId.contains("todoColumn")) return Task.TaskStatus.TODO;
        if (dropId.contains("inProgressColumn")) return Task.TaskStatus.IN_PROGRESS;
        if (dropId.contains("doneColumn")) return Task.TaskStatus.DONE;
        return null;
    }

    public void viewTask(Task task) {
        this.selectedTask = task;
        // Optional: reload task from DB for fresh data if needed
        // this.selectedTask = taskService.findById(task.getId());
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

    public Task.TaskStatus getNewTaskStatus() {
        return newTaskStatus;
    }

    public Task.TaskStatus[] getAvailableStatuses() {
        return Task.TaskStatus.values();
    }

    public Task getSelectedTask() {
        return selectedTask;
    }
}

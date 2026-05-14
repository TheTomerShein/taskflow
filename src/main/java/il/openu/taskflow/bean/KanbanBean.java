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
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.component.UIComponent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityNotFoundException;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

        try {
            Task newTask = taskService.createTask(
                    newTaskTitle.trim(),
                    newTaskDescription != null ? newTaskDescription.trim() : "",
                    newTaskStatus != null ? newTaskStatus : Task.TaskStatus.TODO,
                    currentBoardId,
                    authBean.getCurrentUser().getId(),
                    null
            );

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Created",
                            "Task '" + newTaskTitle + "' created successfully"));

            newTaskTitle = null;
            newTaskDescription = null;
            newTaskStatus = Task.TaskStatus.TODO;
            loadTasks();
        } catch (EntityNotFoundException | UnauthorizedException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to create task: " + e.getMessage()));
        }
    }

    public void onTaskDropFromJS() {
        if (authBean.getCurrentUser() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not authenticated"));
            return;
        }

        String taskIdStr = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("taskId");
        String newStatusStr = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("newStatus");

        if (taskIdStr == null || newStatusStr == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "Invalid drop data"));
            return;
        }

        try {
            Long taskId = Long.parseLong(taskIdStr);
            Task.TaskStatus newStatus = Task.TaskStatus.valueOf(newStatusStr);

            Task existingTask = findTaskById(taskId);
            if (existingTask == null) {
                 FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Task not found in view"));
                 return;
            }

            if (newStatus.equals(existingTask.getStatus())) {
                return;
            }

            taskService.moveTask(taskId, newStatus, authBean.getCurrentUser().getId());
            loadTasks();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Moved",
                            "Task '" + existingTask.getTitle() + "' moved to " + newStatus));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Move failed: " + e.getMessage()));
        }
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



    public void viewTask(Task task) {
        this.selectedTask = task;
        if (task.getAssignee() != null) {
            this.selectedAssigneeId = task.getAssignee().getId();
        } else {
            this.selectedAssigneeId = null;
        }
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

        taskService.updateTask(selectedTask, authBean.getCurrentUser(), oldAssigneeId);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Task updated successfully"));
        loadTasks();
    }

    public void addComment() {
        if (selectedTask == null || newCommentText == null || newCommentText.trim().isEmpty()) return;

        try {
            taskService.addComment(selectedTask.getId(), authBean.getCurrentUser().getId(), newCommentText.trim());
            this.selectedTaskComments = commentRepository.findByTaskId(selectedTask.getId());
            this.newCommentText = "";
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Comment added"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add comment"));
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
}

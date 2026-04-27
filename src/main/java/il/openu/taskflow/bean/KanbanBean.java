package il.openu.taskflow.bean;

import il.openu.taskflow.entity.Task;
import il.openu.taskflow.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.event.DragDropEvent;

import java.util.List;

@Named
@RequestScoped
public class KanbanBean {

    @Inject
    private TaskService taskService;

    @Inject
    private AuthBean authBean;

    private List<Task> todoTasks;
    private List<Task> inProgressTasks;
    private List<Task> doneTasks;

    private Long currentBoardId;

    // שדות לטופס "Add Task" (מודאל)
    private String newTaskTitle;
    private String newTaskDescription;

    @PostConstruct
    public void init() {
        if (currentBoardId != null) {
            loadTasks();
        }
    }

    public void loadTasks() {
        if (currentBoardId != null) {
            todoTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.TODO);
            inProgressTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.IN_PROGRESS);
            doneTasks = taskService.getTasksByStatus(currentBoardId, Task.TaskStatus.DONE);
        } else {
            todoTasks = List.of();
            inProgressTasks = List.of();
            doneTasks = List.of();
        }
    }

    /**
     * יצירת משימה חדשה – נקרא מהמודאל בכל עמודה (ברירת מחדל TODO)
     */
    public void createNewTask() {
        if (currentBoardId == null || newTaskTitle == null || newTaskTitle.trim().isEmpty() || authBean.getCurrentUser() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "שם המשימה חובה"));
            return;
        }

        Task newTask = taskService.createTask(
                newTaskTitle.trim(),
                newTaskDescription != null ? newTaskDescription.trim() : "",
                Task.TaskStatus.TODO,      // אפשר לשנות לפי העמודה שבה לחצו
                currentBoardId,
                authBean.getCurrentUser().getId(),
                null                       // assignee – אפשר להוסיף מאוחר יותר
        );

        if (newTask != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Created", "המשימה '" + newTaskTitle + "' נוצרה"));

            newTaskTitle = null;
            newTaskDescription = null;
            loadTasks(); // רענון ה-Kanban
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "יצירת המשימה נכשלה"));
        }
    }

    // onTaskDrop – נשאר בדיוק כמו שהיה (העתק מהקובץ המקורי שלך)
    public void onTaskDrop(DragDropEvent event) {
        String dragId = event.getDragId();
        String dropId = event.getDropId();

        String[] idParts = dragId.split(":");
        int rowIndex = Integer.parseInt(idParts[idParts.length - 2]);

        Task draggedTask = null;
        if (dragId.contains("todoRepeat")) {
            draggedTask = todoTasks.get(rowIndex);
        } else if (dragId.contains("inProgressRepeat")) {
            draggedTask = inProgressTasks.get(rowIndex);
        } else if (dragId.contains("doneRepeat")) {
            draggedTask = doneTasks.get(rowIndex);
        }

        if (draggedTask == null || authBean.getCurrentUser() == null) {
            return;
        }

        Task.TaskStatus newStatus = getStatusFromDropZone(dropId);

        if (newStatus != null && !newStatus.equals(draggedTask.getStatus())) {
            taskService.moveTask(draggedTask.getId(), newStatus, authBean.getCurrentUser().getId());
            loadTasks();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Task Moved",
                            "המשימה '" + draggedTask.getTitle() + "' הועברה ל-" + newStatus));
        }
    }

    private Task.TaskStatus getStatusFromDropZone(String dropId) {
        if (dropId.contains("todoColumn")) return Task.TaskStatus.TODO;
        if (dropId.contains("inProgressColumn")) return Task.TaskStatus.IN_PROGRESS;
        if (dropId.contains("doneColumn")) return Task.TaskStatus.DONE;
        return null;
    }

    public void setCurrentBoardId(Long currentBoardId) {
        this.currentBoardId = currentBoardId;
        if (currentBoardId != null) {
            loadTasks();
        }
    }

    // Getters & Setters (כולל השדות החדשים)
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
}
package il.openu.taskflow.service;

import il.openu.taskflow.entity.*;
import il.openu.taskflow.repository.*;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Business logic for tasks - create, move (drag & drop), assign, comment, activity logging.
 */
@Stateless
public class TaskService {

    @Inject
    private TaskRepository taskRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private BoardRepository boardRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ActivityLogRepository activityLogRepository;

    @Inject
    private JmsProducer jmsProducer;

    /**
     * Creates a new task in a specific board.
     *
     * @param title       task title
     * @param description task description
     * @param boardId     board ID
     * @param createdById user who creates the task
     * @return the created task or null if failed
     */
    public Task createTask(String title, String description, Task.TaskStatus initialStatus,
                           Long boardId, Long createdById, Long assigneeId) {
        Board board = boardRepository.findById(boardId);
        User createdBy = userRepository.findById(createdById);

        if (board == null || createdBy == null) {
            return null;
        }

        if (!isProjectMember(createdById, board.getProject().getId())) {
            return null;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setStatus(initialStatus != null ? initialStatus : Task.TaskStatus.TODO);
        task.setBoard(board);
        task.setCreatedBy(createdBy);

        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId);
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);

        createActivityLog(board.getProject(), createdBy, ActivityLog.ActionType.TASK_CREATED,
                "Task created: " + title, savedTask);

        jmsProducer.sendTaskEvent("TASK_CREATED", savedTask.getId(), createdById);

        return savedTask;
    }

    /**
     * Moves a task to a new status (used by Drag & Drop in Kanban board).
     *
     * @param taskId    task ID
     * @param newStatus new status (TODO / IN_PROGRESS / DONE)
     * @param userId    user who performed the action
     * @return updated task or null if failed
     */
    public Task moveTask(Long taskId, Task.TaskStatus newStatus, Long userId) {
        Task task = taskRepository.findById(taskId);
        User user = userRepository.findById(userId);

        if (task == null || user == null) {
            return null;
        }

        // Check if the user is a member of the project
        if (!isProjectMember(userId, task.getBoard().getProject().getId())) {
            return null;
        }

        Task.TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        Task updatedTask = taskRepository.update(task);

        // Log the status change
        createActivityLog(task.getBoard().getProject(), user, ActivityLog.ActionType.STATUS_CHANGED,
                "Task moved from " + oldStatus + " to " + newStatus, updatedTask);

        jmsProducer.sendTaskEvent("STATUS_CHANGED", taskId, userId);

        return updatedTask;
    }

    /**
     * Adds a comment to a task.
     *
     * @param taskId  task ID
     * @param userId  user who wrote the comment
     * @param content comment text
     * @return the created comment or null if failed
     */
    public Comment addComment(Long taskId, Long userId, String content) {
        Task task = taskRepository.findById(taskId);
        User user = userRepository.findById(userId);

        if (task == null || user == null) {
            return null;
        }

        // Check if the user is a member of the project
        if (!isProjectMember(userId, task.getBoard().getProject().getId())) {
            return null;
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        // Create activity log
        createActivityLog(task.getBoard().getProject(), user, ActivityLog.ActionType.COMMENT_ADDED,
                "Comment added to task: " + task.getTitle(), task);

        jmsProducer.sendTaskEvent("COMMENT_ADDED", taskId, userId);

        return savedComment;
    }

    /**
     * Internal helper method to create and persist activity log entries.
     *
     * @param project    the project associated with the activity
     * @param user       the user who performed the action
     * @param actionType the type of action being logged
     * @param details    a descriptive string of the action
     * @param task       the task related to the activity
     */
    private void createActivityLog(Project project, User user, ActivityLog.ActionType actionType,
                                   String details, Task task) {
        ActivityLog log = new ActivityLog(project, user, actionType, details);
        log.setTask(task);                    // now works because we added the field
        activityLogRepository.save(log);
    }

    /**
     * Checks if a specific user is a member of a project.
     *
     * @param userId    the ID of the user to check
     * @param projectId the ID of the project
     * @return true if the user is a member of the project, false otherwise
     */
    private boolean isProjectMember(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) return false;
        return project.getMembers().stream()
                .anyMatch(u -> u.getId().equals(userId));
    }

    /**
     * Retrieves a list of tasks for a specific board filtered by their status.
     *
     * @param boardId the ID of the board
     * @param status  the status to filter by (e.g., TODO, IN_PROGRESS, DONE)
     * @return a list of tasks matching the criteria
     */
    public List<Task> getTasksByStatus(Long boardId, Task.TaskStatus status) {
        return taskRepository.findByBoardIdAndStatus(boardId, status);
    }
}
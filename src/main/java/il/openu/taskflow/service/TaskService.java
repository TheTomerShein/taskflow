package il.openu.taskflow.service;

import il.openu.taskflow.entity.*;
import il.openu.taskflow.repository.*;
import il.openu.taskflow.exception.UnauthorizedException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;

/**
 * Business logic for tasks - create, move (drag & drop), assign, comment.
 * Activity logging is handled asynchronously via JMS + TaskEventMDB triggered after transaction commit.
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
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));
        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!isProjectMember(createdById, board.getProject().getId())) {
            throw new UnauthorizedException("User is not a member of this project");
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setStatus(initialStatus != null ? initialStatus : Task.TaskStatus.TODO);
        task.setBoard(board);
        task.setCreatedBy(createdBy);

        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);

        // Send async JMS event
        jmsProducer.sendEvent(
                "TASK_CREATED",
                board.getProject().getId(),
                boardId,
                savedTask.getId(),
                createdById,
                "Task created: " + title
        );

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
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if the user is a member of the project
        if (!isProjectMember(userId, task.getBoard().getProject().getId())) {
            throw new UnauthorizedException("User is not a member of this project");
        }

        Task.TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        Task updatedTask = taskRepository.update(task);

        // Send async JMS event
        jmsProducer.sendEvent(
                "STATUS_CHANGED",
                task.getBoard().getProject().getId(),
                task.getBoard().getId(),
                taskId,
                userId,
                "Task moved from " + oldStatus + " to " + newStatus
        );

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
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if the user is a member of the project
        if (!isProjectMember(userId, task.getBoard().getProject().getId())) {
            throw new UnauthorizedException("User is not a member of this project");
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        // Send async JMS event
        jmsProducer.sendEvent(
                "COMMENT_ADDED",
                task.getBoard().getProject().getId(),
                task.getBoard().getId(),
                taskId,
                userId,
                "Comment added to task: " + task.getTitle()
        );

        return savedComment;
    }

    /**
     * Updates a task's details (assignee, description, due date) and sends async event.
     *
     * @param task           the task to update (already modified by the bean)
     * @param user           the user who performed the update
     * @param oldAssigneeId  the previous assignee ID (null if none), used for change detection
     */
    public Task updateTask(Task task, User user, Long oldAssigneeId) {
        Board board = task.getBoard();

        // Detect assignee change
        Long newAssigneeId = (task.getAssignee() != null) ? task.getAssignee().getId() : null;
        boolean assigneeChanged = (oldAssigneeId == null && newAssigneeId != null)
                || (oldAssigneeId != null && !oldAssigneeId.equals(newAssigneeId));

        Task updatedTask = taskRepository.update(task);

        if (assigneeChanged) {
            String assigneeName = (task.getAssignee() != null)
                    ? task.getAssignee().getUsername()
                    : "Unassigned";

            // Send async JMS event
            jmsProducer.sendEvent(
                    "TASK_ASSIGNED",
                    board.getProject().getId(),
                    board.getId(),
                    updatedTask.getId(),
                    user.getId(),
                    "Task '" + task.getTitle() + "' assigned to " + assigneeName
            );
        } else {
            // Send async JMS event
            jmsProducer.sendEvent(
                    "TASK_UPDATED",
                    board.getProject().getId(),
                    board.getId(),
                    updatedTask.getId(),
                    user.getId(),
                    "Task updated: " + task.getTitle()
            );
        }

        return updatedTask;
    }

    /**
     * Checks if a specific user is a member of a project.
     *
     * @param userId    the ID of the user to check
     * @param projectId the ID of the project
     * @return true if the user is a member of the project, false otherwise
     */
    private boolean isProjectMember(Long userId, Long projectId) {
        return projectRepository.findById(projectId)
                .map(project -> project.getMembers().stream().anyMatch(u -> u.getId().equals(userId)))
                .orElse(false);
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
package il.openu.taskflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enum for all possible actions
    public enum ActionType {
        PROJECT_CREATED,
        PROJECT_UPDATED,
        MEMBER_ADDED,
        TASK_CREATED,
        TASK_UPDATED,
        TASK_ASSIGNED,
        STATUS_CHANGED,
        COMMENT_ADDED,
        BOARD_CREATED,
        DUE_DATE_CHANGED
    }

    public ActivityLog() {
    }

    // Simple constructor for easy use
    public ActivityLog(Project project, User user, ActionType actionType, String details) {
        this.project = project;
        this.user = user;
        this.actionType = actionType;
        this.details = details;
    }

    // Constructor with Board
    public ActivityLog(Project project, Board board, User user, ActionType actionType, String details) {
        this.project = project;
        this.board = board;
        this.user = user;
        this.actionType = actionType;
        this.details = details;
    }

    // These methods run automatically when saving or updating
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Task getTask() { return task; }

    public void setTask(Task task) { this.task = task; }

    public Board getBoard() { return board; }

    public void setBoard(Board board) { this.board = board; }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
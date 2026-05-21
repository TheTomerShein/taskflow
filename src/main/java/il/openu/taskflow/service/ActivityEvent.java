package il.openu.taskflow.service;

/**
 * Event class to carry activity log payload across threads/transactions via CDI.
 */
public class ActivityEvent {
    private final String eventType;
    private final Long projectId;
    private final Long boardId;
    private final Long taskId;
    private final Long userId;
    private final String details;

    public ActivityEvent(String eventType, Long projectId, Long boardId, Long taskId, Long userId, String details) {
        this.eventType = eventType;
        this.projectId = projectId;
        this.boardId = boardId;
        this.taskId = taskId;
        this.userId = userId;
        this.details = details;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getBoardId() {
        return boardId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDetails() {
        return details;
    }
}

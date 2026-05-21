package il.openu.taskflow.service;

import il.openu.taskflow.entity.ActivityLog;
import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.Task;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ActivityLogRepository;
import il.openu.taskflow.repository.BoardRepository;
import il.openu.taskflow.repository.ProjectRepository;
import il.openu.taskflow.repository.TaskRepository;
import il.openu.taskflow.repository.UserRepository;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;

/**
 * Message-Driven Bean that consumes activity events from the JMS queue
 * and persists them as ActivityLog entries asynchronously.
 * Handles task, board, and project-level events.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(
        propertyName = "destinationLookup",
        propertyValue = "jms/TaskEventsQueue"),
    @ActivationConfigProperty(
        propertyName = "destinationType",
        propertyValue = "jakarta.jms.Queue")
})
public class TaskEventMDB implements MessageListener {

    @Inject
    private ActivityLogRepository activityLogRepository;

    @Inject
    private TaskRepository taskRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private BoardRepository boardRepository;

    @Inject
    private ProjectRepository projectRepository;

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                System.err.println("[MDB] Received non-TextMessage. Ignoring.");
                return;
            }

            String jsonText = ((TextMessage) message).getText();
            System.out.println("[MDB] Received JSON Event: " + jsonText);

            // Parse JSON
            JsonObject json;
            try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
                json = reader.readObject();
            }

            String eventType = json.getString("eventType", "UNKNOWN");

            // Robust numeric extraction — prevents NPE / unnecessary redeliveries
            Long projectId = null;
            if (json.containsKey("projectId") && !json.isNull("projectId")) {
                long val = json.getJsonNumber("projectId").longValue();
                if (val != 0) projectId = val;
            }

            Long boardId = null;
            if (json.containsKey("boardId") && !json.isNull("boardId")) {
                long val = json.getJsonNumber("boardId").longValue();
                if (val != 0) boardId = val;
            }

            Long taskId = null;
            if (json.containsKey("taskId") && !json.isNull("taskId")) {
                long val = json.getJsonNumber("taskId").longValue();
                if (val != 0) taskId = val;
            }

            Long userId = null;
            if (json.containsKey("userId") && !json.isNull("userId")) {
                long val = json.getJsonNumber("userId").longValue();
                if (val != 0) userId = val;
            }

            String details = json.getString("details", "");

            // Load entities — best-effort enrichment
            User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;
            Board board = (boardId != null) ? boardRepository.findById(boardId).orElse(null) : null;
            Task task = (taskId != null) ? taskRepository.findById(taskId).orElse(null) : null;
            Project project = (projectId != null) ? projectRepository.findById(projectId).orElse(null) : null;

            // Fallback: derive project from board if not explicitly passed
            if (project == null && board != null) {
                project = board.getProject();
            }

            // User and Project are mandatory for any ActivityLog entry
            if (user == null || project == null) {
                System.err.println("[MDB] User or Project not found. Dropping event: " + jsonText);
                return; // do NOT throw here — avoids infinite redelivery on bad data
            }

            ActivityLog.ActionType actionType = mapToActionType(eventType);

            // Choose the correct constructor based on whether a board is present
            ActivityLog log;
            if (board != null) {
                log = new ActivityLog(project, board, user, actionType, details);
            } else {
                log = new ActivityLog(project, user, actionType, details);
            }

            if (task != null) {
                log.setTask(task);
            }

            activityLogRepository.save(log);
            System.out.println("[MDB] ActivityLog saved successfully: " + actionType);

        } catch (Exception e) {
            System.err.println("[MDB] ERROR processing message: " + e.getMessage());
            e.printStackTrace();

            // Throw RuntimeException so the container can redeliver the message.
            // Configure redelivery limits + DLQ in Payara for production safety.
            throw new RuntimeException("Failed to process event message", e);
        }
    }

    /**
     * Maps an event type string to the corresponding ActivityLog.ActionType enum value.
     * Covers task, board, and project-level events.
     *
     * @param eventType the event type string from the JSON payload
     * @return the matching ActionType, defaults to TASK_UPDATED for unknown types
     */
    private ActivityLog.ActionType mapToActionType(String eventType) {
        if (eventType == null) {
            return ActivityLog.ActionType.TASK_UPDATED;
        }
        switch (eventType) {
            // Task events
            case "TASK_CREATED":
                return ActivityLog.ActionType.TASK_CREATED;
            case "STATUS_CHANGED":
                return ActivityLog.ActionType.STATUS_CHANGED;
            case "COMMENT_ADDED":
                return ActivityLog.ActionType.COMMENT_ADDED;
            case "TASK_UPDATED":
                return ActivityLog.ActionType.TASK_UPDATED;
            case "TASK_ASSIGNED":
                return ActivityLog.ActionType.TASK_ASSIGNED;

            // Board events
            case "BOARD_CREATED":
                return ActivityLog.ActionType.BOARD_CREATED;

            // Project events
            case "PROJECT_CREATED":
                return ActivityLog.ActionType.PROJECT_CREATED;
            case "PROJECT_UPDATED":
                return ActivityLog.ActionType.PROJECT_UPDATED;
            case "MEMBER_ADDED":
                return ActivityLog.ActionType.MEMBER_ADDED;
            case "MEMBER_REMOVED":
                return ActivityLog.ActionType.MEMBER_REMOVED;

            default:
                System.err.println("[MDB] Unknown event type: " + eventType);
                return ActivityLog.ActionType.TASK_UPDATED;
        }
    }
}

package il.openu.taskflow.service;

import il.openu.taskflow.entity.ActivityLog;
import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.Task;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ActivityLogRepository;
import il.openu.taskflow.repository.BoardRepository;
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
 * Message-Driven Bean that consumes task events from the JMS queue
 * and persists them as ActivityLog entries asynchronously.
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

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                System.err.println("[MDB] Received non-TextMessage. Ignoring.");
                return;
            }

            String jsonText = ((TextMessage) message).getText();
            System.out.println("[MDB] Received JSON Event: " + jsonText);

            // Parse JSON with robust handling
            JsonObject json;
            try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
                json = reader.readObject();
            }

            String eventType = json.getString("eventType", "UNKNOWN");

            // Robust numeric extraction (prevents NPE / unnecessary redeliveries)
            Long taskId = null;
            if (json.containsKey("taskId") && !json.isNull("taskId")) {
                taskId = json.getJsonNumber("taskId").longValue();
            }

            Long userId = null;
            if (json.containsKey("userId") && !json.isNull("userId")) {
                userId = json.getJsonNumber("userId").longValue();
            }

            Long boardId = null;
            if (json.containsKey("boardId") && !json.isNull("boardId")) {
                boardId = json.getJsonNumber("boardId").longValue();
            }

            String details = json.getString("details", "");

            // Load entities (best-effort enrichment)
            User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;
            Board board = (boardId != null) ? boardRepository.findById(boardId).orElse(null) : null;
            Task task = (taskId != null) ? taskRepository.findById(taskId).orElse(null) : null;

            if (user == null || board == null) {
                System.err.println("[MDB] User or Board not found. Event: " + jsonText);
                return; // Do not throw here — bad data, avoid infinite redelivery loop
            }

            Project project = board.getProject();

            // Map to enum
            ActivityLog.ActionType actionType = mapToActionType(eventType);

            // Create ActivityLog using existing constructor + lifecycle callbacks
            // (no manual timestamp — entity uses createdAt via @PrePersist)
            ActivityLog log = new ActivityLog(project, board, user, actionType, details);
            if (task != null) {
                log.setTask(task);
            }

            activityLogRepository.save(log);

            System.out.println("[MDB] ActivityLog saved successfully: " + actionType);

        } catch (Exception e) {
            System.err.println("[MDB] ERROR processing message: " + e.getMessage());
            e.printStackTrace();

            // Throw RuntimeException so the container redelivers the message
            // (configure redelivery limits + DLQ in Payara for production safety)
            throw new RuntimeException("Failed to process TaskEvent message", e);
        }
    }

    /**
     * Maps an event type string to the corresponding ActivityLog.ActionType enum value.
     *
     * @param eventType the event type string from the JSON payload
     * @return the matching ActionType, defaults to TASK_UPDATED for unknown types
     */
    private ActivityLog.ActionType mapToActionType(String eventType) {
        if (eventType == null) {
            return ActivityLog.ActionType.TASK_UPDATED;
        }
        switch (eventType) {
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
            default:
                System.err.println("[MDB] Unknown event type: " + eventType);
                return ActivityLog.ActionType.TASK_UPDATED;
        }
    }
}

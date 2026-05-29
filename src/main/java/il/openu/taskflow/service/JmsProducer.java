package il.openu.taskflow.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * JMS producer for sending activity events as JSON messages to the TaskEventsQueue.
 * Supports task, board, and project-level events.
 *
 * The @JMSDestinationDefinition annotation automatically creates the JMS queue
 * in the application server when the application is deployed — no manual
 * Payara admin setup or glassfish-resources.xml is required.
 */
@JMSDestinationDefinition(
        name = "jms/TaskEventsQueue",
        interfaceName = "jakarta.jms.Queue",
        destinationName = "TaskEventsQueue",
        description = "Queue for async activity log events"
)
@Stateless
public class JmsProducer {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "jms/TaskEventsQueue", type = Queue.class)
    private Queue taskEventsQueue;

    /**
     * Sends a generic structured JSON event to the TaskEventsQueue.
     * Runs with NOT_SUPPORTED so the JMS send is NOT enlisted in the caller's transaction.
     * This means the JMS broker delivers the message only AFTER the caller's DB transaction
     * has committed, preventing the MDB from consuming the message before entities are visible in DB.
     *
     * @param eventType type of event (e.g. TASK_CREATED, BOARD_CREATED, PROJECT_CREATED)
     * @param projectId the project ID associated with the event (may be null)
     * @param boardId   the board ID associated with the event (may be null)
     * @param taskId    the task ID associated with the event (may be null)
     * @param userId    the user who triggered the event
     * @param details   human-readable description of the event
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendEvent(String eventType, Long projectId, Long boardId, Long taskId, Long userId, String details) {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("eventType", eventType)
                    .add("projectId", projectId != null ? projectId : 0)
                    .add("boardId", boardId != null ? boardId : 0)
                    .add("taskId", taskId != null ? taskId : 0)
                    .add("userId", userId != null ? userId : 0)
                    .add("details", details != null ? details : "");

            String jsonPayload = builder.build().toString();

            jmsContext.createProducer().send(taskEventsQueue, jsonPayload);
            System.out.println("[JMS] Event sent: " + jsonPayload);

        } catch (Exception e) {
            System.err.println("[JMS] Failed to send event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Backwards-compatible wrapper for TaskService.
     * Delegates to sendEvent with null projectId.
     *
     * @param eventType type of event (e.g. TASK_CREATED, STATUS_CHANGED)
     * @param taskId    the task ID (may be null)
     * @param userId    the user who triggered the event
     * @param boardId   the board ID associated with the event
     * @param details   human-readable description of the event
     */
    public void sendTaskEvent(String eventType, Long taskId, Long userId, Long boardId, String details) {
        sendEvent(eventType, null, boardId, taskId, userId, details);
    }


}
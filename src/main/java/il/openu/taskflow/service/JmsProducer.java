package il.openu.taskflow.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * JMS producer for sending task events as JSON messages to the TaskEventsQueue.
 */
@Stateless
public class JmsProducer {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "jms/TaskEventsQueue")
    private Queue taskEventsQueue;

    /**
     * Sends a structured JSON event to the TaskEventsQueue.
     *
     * @param eventType type of event (e.g. TASK_CREATED, STATUS_CHANGED)
     * @param taskId    the task ID (may be null for non-task events)
     * @param userId    the user who triggered the event
     * @param boardId   the board ID associated with the event
     * @param details   human-readable description of the event
     */
    public void sendTaskEvent(String eventType, Long taskId, Long userId, Long boardId, String details) {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("eventType", eventType)
                    .add("taskId", taskId != null ? taskId : 0)
                    .add("userId", userId != null ? userId : 0)
                    .add("boardId", boardId != null ? boardId : 0)
                    .add("details", details != null ? details : "");

            String jsonPayload = builder.build().toString();

            jmsContext.createProducer().send(taskEventsQueue, jsonPayload);
            System.out.println("[JMS] Event sent: " + jsonPayload);

        } catch (Exception e) {
            System.err.println("[JMS] Failed to send event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
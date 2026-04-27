package il.openu.taskflow.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;

@Stateless
public class JmsProducer {

    // הזרקת הקשר ה-JMS (מחליף את ה-ConnectionFactory הישן)
    @Inject
    private JMSContext jmsContext;

    // הזרקת התור (Queue) שיצרת בשרת ה-Payara
    @Resource(lookup = "jms/TaskEventsQueue")
    private Queue taskEventsQueue;

    /**
     * פונקציה לשליחת אירוע המשימה לתור
     */
    public void sendTaskEvent(String eventType, Long taskId, Long userId) {
        try {
            String messagePayload = String.format("Event: %s, TaskID: %d, UserID: %d", eventType, taskId, userId);

            // שליחת ההודעה לתור
            jmsContext.createProducer().send(taskEventsQueue, messagePayload);

            System.out.println("JMS Message sent successfully: " + messagePayload);
        } catch (Exception e) {
            System.err.println("Failed to send JMS message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
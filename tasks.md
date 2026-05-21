# Tasks for Migrating BoardService and ProjectService Logging to JMS

This file outlines the necessary tasks and code modifications required to migrate `BoardService` and `ProjectService` from synchronous database activity logging to fully asynchronous JMS + MDB logging.

---

## Checklist / Implementation Tasks

- [x] **Step 1: Update `JmsProducer.java`**
  - Add an overloaded or generalized `sendEvent` method supporting `projectId`.
  - Maintain backwards compatibility for `TaskService` by preserving `sendTaskEvent`.
- [x] **Step 2: Update `TaskEventMDB.java`**
  - Inject `ProjectRepository`.
  - Read `projectId` from the JSON payload.
  - Gracefully handle situations where `board` or `task` are `null` (since project-level actions do not involve a specific board or task).
  - Map new board/project action types.
  - Choose the correct `ActivityLog` constructor based on whether a `board` is present.
- [x] **Step 3: Refactor `BoardService.java`**
  - Inject `JmsProducer`.
  - Remove direct dependency on `ActivityLogRepository`.
  - Replace synchronous logging with async JMS events.
- [x] **Step 4: Refactor `ProjectService.java`**
  - Inject `JmsProducer`.
  - Remove direct dependency on `ActivityLogRepository`.
  - Replace all synchronous logging occurrences (`createProject`, `addMember`, `removeMember`, `updateProject`) with async JMS events.
- [x] **Step 5: Verification**
  - Run `mvn clean compile` to ensure the project builds successfully. ✅

---

## Detailed Code Modifications

### 1. `JmsProducer.java`

Add a new `sendEvent` method that includes the `projectId` and update `sendTaskEvent` to call it.

```java
// Add to il.openu.taskflow.service.JmsProducer

    /**
     * Sends a generic structured JSON event to the TaskEventsQueue.
     */
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
     * Overloaded method for backwards compatibility with TaskService.
     */
    public void sendTaskEvent(String eventType, Long taskId, Long userId, Long boardId, String details) {
        sendEvent(eventType, null, boardId, taskId, userId, details);
    }
```

---

### 2. `TaskEventMDB.java`

Update the Message-Driven Bean to handle project-level events (which have no board or task) and look up projects directly.

```java
// Inject ProjectRepository in il.openu.taskflow.service.TaskEventMDB
    @Inject
    private ProjectRepository projectRepository;

// Update onMessage() logic:
    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                System.err.println("[MDB] Received non-TextMessage. Ignoring.");
                return;
            }

            String jsonText = ((TextMessage) message).getText();
            System.out.println("[MDB] Received JSON Event: " + jsonText);

            JsonObject json;
            try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
                json = reader.readObject();
            }

            String eventType = json.getString("eventType", "UNKNOWN");

            Long projectId = null;
            if (json.containsKey("projectId") && !json.isNull("projectId")) {
                projectId = json.getJsonNumber("projectId").longValue();
            }

            Long boardId = null;
            if (json.containsKey("boardId") && !json.isNull("boardId")) {
                boardId = json.getJsonNumber("boardId").longValue();
            }

            Long taskId = null;
            if (json.containsKey("taskId") && !json.isNull("taskId")) {
                taskId = json.getJsonNumber("taskId").longValue();
            }

            Long userId = null;
            if (json.containsKey("userId") && !json.isNull("userId")) {
                userId = json.getJsonNumber("userId").longValue();
            }

            String details = json.getString("details", "");

            // Fetch entities
            User user = (userId != null && userId != 0) ? userRepository.findById(userId).orElse(null) : null;
            Board board = (boardId != null && boardId != 0) ? boardRepository.findById(boardId).orElse(null) : null;
            Task task = (taskId != null && taskId != 0) ? taskRepository.findById(taskId).orElse(null) : null;
            Project project = (projectId != null && projectId != 0) ? projectRepository.findById(projectId).orElse(null) : null;

            // Fallback project lookup if not explicitly passed
            if (project == null && board != null) {
                project = board.getProject();
            }

            // A valid user and project are minimum requirements for any log entry
            if (user == null || project == null) {
                System.err.println("[MDB] User or Project not found. Event: " + jsonText);
                return; 
            }

            ActivityLog.ActionType actionType = mapToActionType(eventType);

            // Construct ActivityLog (using appropriate constructor depending on board presence)
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
            throw new RuntimeException("Failed to process event message", e);
        }
    }

// Update mapToActionType() to map new event types:
    private ActivityLog.ActionType mapToActionType(String eventType) {
        if (eventType == null) {
            return ActivityLog.ActionType.TASK_UPDATED;
        }
        switch (eventType) {
            // Task Events
            case "TASK_CREATED": return ActivityLog.ActionType.TASK_CREATED;
            case "STATUS_CHANGED": return ActivityLog.ActionType.STATUS_CHANGED;
            case "COMMENT_ADDED": return ActivityLog.ActionType.COMMENT_ADDED;
            case "TASK_UPDATED": return ActivityLog.ActionType.TASK_UPDATED;
            case "TASK_ASSIGNED": return ActivityLog.ActionType.TASK_ASSIGNED;
            
            // Board Events
            case "BOARD_CREATED": return ActivityLog.ActionType.BOARD_CREATED;
            
            // Project Events
            case "PROJECT_CREATED": return ActivityLog.ActionType.PROJECT_CREATED;
            case "PROJECT_UPDATED": return ActivityLog.ActionType.PROJECT_UPDATED;
            case "MEMBER_ADDED": return ActivityLog.ActionType.MEMBER_ADDED;
            case "MEMBER_REMOVED": return ActivityLog.ActionType.MEMBER_REMOVED;
            
            default:
                System.err.println("[MDB] Unknown event type: " + eventType);
                return ActivityLog.ActionType.TASK_UPDATED;
        }
    }
```

---

### 3. `BoardService.java`

Replace direct database interaction with JMS event publishing.

```java
// In BoardService.java

    @Inject
    private JmsProducer jmsProducer;

    // Remove direct injection of:
    // private ActivityLogRepository activityLogRepository;

    public Board createBoard(String name, Project project, User user) {
        Board board = new Board();
        board.setName(name);
        board.setProject(project);

        Board savedBoard = boardRepository.save(board);

        // Async logging via JMS
        jmsProducer.sendEvent(
                "BOARD_CREATED",
                project.getId(),
                savedBoard.getId(),
                null,
                user.getId(),
                "Board created: " + name
        );

        return savedBoard;
    }
```

---

### 4. `ProjectService.java`

Replace all synchronous logging methods with async event messaging.

```java
// In ProjectService.java

    @Inject
    private JmsProducer jmsProducer;

    // Remove direct injection of:
    // private ActivityLogRepository activityLogRepository;

    public Project createProject(String name, String description, User owner) {
        // ... save logic ...

        // Async log: PROJECT_CREATED
        jmsProducer.sendEvent("PROJECT_CREATED", savedProject.getId(), null, null, owner.getId(), "Project created: " + name);
        return savedProject;
    }

    public boolean addMember(Long projectId, Long userId, User addedBy) {
        // ... member logic ...

        // Async log: MEMBER_ADDED
        jmsProducer.sendEvent("MEMBER_ADDED", project.getId(), null, null, addedBy.getId(), "Member added: " + user.getUsername());
        return true;
    }

    public boolean removeMember(Long projectId, Long userId, User removedBy) {
        // ... remove logic ...

        // Async log: MEMBER_REMOVED
        jmsProducer.sendEvent("MEMBER_REMOVED", project.getId(), null, null, removedBy.getId(), "Member removed: " + user.getUsername());
        return true;
    }

    public Project updateProject(Project project, User user) {
        Project updated = projectRepository.update(project);

        // Async log: PROJECT_UPDATED
        jmsProducer.sendEvent("PROJECT_UPDATED", updated.getId(), null, null, user.getId(), "Project updated: " + project.getName());
        return updated;
    }
```

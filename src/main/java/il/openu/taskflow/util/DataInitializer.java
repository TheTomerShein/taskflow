package il.openu.taskflow.util;

import il.openu.taskflow.entity.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import il.openu.taskflow.util.PasswordUtil;

@Singleton
@Startup
public class DataInitializer {

    private static final Logger logger = Logger.getLogger(DataInitializer.class.getName());

    @PersistenceContext(unitName = "TaskFlowPU")
    private EntityManager em;

    @PostConstruct
    public void init() {
        // System.out.println goes directly to the console output, bypassing java.util.logging
        System.out.println("🚀 [System.out] DataInitializer @PostConstruct started...");
        logger.info("🚀 [Logger] DataInitializer @PostConstruct started...");

        try {
            long userCount = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
            System.out.println("📊 [System.out] Current users in DB: " + userCount);
            logger.info("📊 Current users in DB: " + userCount);

            if (userCount == 0) {
                System.out.println("🌱 [System.out] Creating seed data...");
                logger.info("🌱 Creating seed data...");
                createTestData();
            } else {
                System.out.println("✅ [System.out] Seed data already exists - skipping creation");
                logger.info("✅ Seed data already exists - skipping creation");
            }

            System.out.println("✅ [System.out] DataInitializer finished successfully!");
            logger.info("✅ DataInitializer finished successfully!");

        } catch (Exception e) {
            System.err.println("❌ [System.err] ERROR in DataInitializer: " + e.getMessage());
            logger.log(Level.SEVERE, "❌ ERROR in DataInitializer", e);
        }
    }

    private void createTestData() {
        // Users
        User tom = new User("tomer", "tomer@openu.ac.il", PasswordUtil.hash("123456"));
        User chen = new User("chen", "chen@openu.ac.il", PasswordUtil.hash("123456"));
        User test = new User("test", "test@test.com", PasswordUtil.hash("123456"));
        em.persist(tom);
        em.persist(chen);
        em.persist(test);

        // Projects + Boards + Tasks
        Project project1 = new Project();
        project1.setName("TaskFlow Development");
        project1.setDescription("פיתוח הפרויקט של הקורס");
        project1.setOwner(tom);
        project1.getMembers().add(tom);
        project1.getMembers().add(chen);
        em.persist(project1);

        Board board1 = new Board();
        board1.setName("Sprint 1");
        board1.setProject(project1);
        em.persist(board1);

        Task task1 = new Task();
        task1.setTitle("Setup JSF + PrimeFaces");
        task1.setDescription("Create basic web project with PrimeFaces");
        task1.setStatus(Task.TaskStatus.TODO);
        task1.setBoard(board1);
        task1.setCreatedBy(tom);
        em.persist(task1);

        System.out.println("✅ [System.out] TaskFlow Seed Data loaded successfully!");
        logger.info("✅ TaskFlow Seed Data loaded successfully! (3 users, 1 project, 1 board, 1 task)");
    }
}
package il.openu.taskflow.repository;

import il.openu.taskflow.entity.User;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 * Repository for User entity - handles all database operations related to users.
 */
@Stateless
public class UserRepository extends AbstractRepository<User> {

    public UserRepository() {
        super(User.class);
    }

    /**
     * Finds user by username (used for login).
     * @param username the username to search
     * @return User object or null if not found
     */
    public User findByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Finds user by email.
     * @param email the email to search
     * @return User object or null if not found
     */
    public User findByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
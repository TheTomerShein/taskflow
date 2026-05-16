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

    /**
     * Searches users by username or email (case-insensitive, partial match).
     * @param term the search term
     * @return list of matching users
     */
    public List<User> searchByTerm(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return em.createQuery(
                        "SELECT u FROM User u WHERE LOWER(u.username) LIKE :pattern OR LOWER(u.email) LIKE :pattern",
                        User.class)
                .setParameter("pattern", pattern)
                .getResultList();
    }
}
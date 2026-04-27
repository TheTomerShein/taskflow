package il.openu.taskflow.service;

import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.UserRepository;
import il.openu.taskflow.util.PasswordUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.Serializable;

/**
 * Business logic for users (login, register, etc.)
 */
@Stateless
public class UserService implements Serializable {

    @Inject
    private UserRepository userRepository;

    /**
     * Register a new user with hashed password.
     * @param username username
     * @param email email
     * @param plainPassword plain text password
     * @return the created user or null if username/email already exists
     */
    public User register(String username, String email, String plainPassword) {
        if (userRepository.findByUsername(username) != null ||
                userRepository.findByEmail(email) != null) {
            return null;
        }

        User user = new User(username, email, PasswordUtil.hash(plainPassword));
        return userRepository.save(user);
    }

    /**
     * Login with password verification (using BCrypt).
     * @param username username
     * @param plainPassword plain text password
     * @return User if successful, null otherwise
     */
    public User login(String username, String plainPassword) {
        User user = userRepository.findByUsername(username);
        if (user != null && PasswordUtil.verify(plainPassword, user.getPassword())) {
            return user;
        }
        return null;
    }

    /**
     * Find user by ID.
     */
    public User findById(Long id) {
        return userRepository.findById(id);
    }
}
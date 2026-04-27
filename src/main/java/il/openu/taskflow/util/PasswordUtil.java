package il.openu.taskflow.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Simple utility for password hashing and verification using BCrypt.
 */
public class PasswordUtil {

    /**
     * Hashes a plain password.
     * @param plainPassword the password in plain text
     * @return hashed password (never null)
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Checks if plain password matches the hashed one.
     * @param plainPassword password entered by user
     * @param hashedPassword password stored in DB
     * @return true if they match
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
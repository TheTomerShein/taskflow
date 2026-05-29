package il.openu.taskflow.bean;

import il.openu.taskflow.entity.User;
import il.openu.taskflow.service.UserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;

@Named
@SessionScoped
public class AuthBean implements Serializable {

    @Inject
    private UserService userService;

    private String username;
    private String email;
    private String password;
    private User currentUser;

    // ==================== LOGIN ====================
    public String login() {
        User user = userService.login(username, password);

        if (user != null) {
            this.currentUser = user;
            addMessage(FacesMessage.SEVERITY_INFO, "התחברות הצליחה", "ברוך הבא, " + username);

            // Clean credentials after successful login submit
            this.username = null;
            this.password = null;

            return "dashboard?faces-redirect=true";
        } else {
            addMessage(FacesMessage.SEVERITY_ERROR, "התחברות נכשלה", "שם משתמש או סיסמה שגויים");
            
            // Clean credentials on failure as well
            this.username = null;
            this.password = null;
            
            return null;
        }
    }

    // ==================== REGISTER ====================
    public String register() {
        // Email validation
        if (email == null || !email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            addMessage(FacesMessage.SEVERITY_ERROR, "הרשמה נכשלה", "כתובת האימייל אינה תקינה");
            
            // Clean fields on failure
            this.username = null;
            this.email = null;
            this.password = null;
            return null;
        }

        // Password validation: 6 to 10 characters, at least one uppercase letter, and at least one digit
        if (password == null || password.length() < 6 || password.length() > 10 
                || !password.matches(".*[A-Z].*") || !password.matches(".*[0-9].*")) {
            addMessage(FacesMessage.SEVERITY_ERROR, "הרשמה נכשלה", "הסיסמה חייבת להיות באורך של 6 עד 10 תווים, להכיל לפחות אות אחת גדולה וספרה אחת");
            
            // Clean fields on failure
            this.username = null;
            this.email = null;
            this.password = null;
            return null;
        }

        User newUser = userService.register(username, email, password);

        if (newUser != null) {
            addMessage(FacesMessage.SEVERITY_INFO, "הרשמה הצליחה", "כעת ניתן להתחבר למערכת");

            // Clean fields after successful registration submit
            this.username = null;
            this.email = null;
            this.password = null;

            return "login?faces-redirect=true";
        } else {
            addMessage(FacesMessage.SEVERITY_ERROR, "הרשמה נכשלה", "שם המשתמש או האימייל כבר קיימים במערכת");
            
            // Clean fields on failure as well
            this.username = null;
            this.email = null;
            this.password = null;
            
            return null;
        }
    }

    public String logout() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance()
                .getExternalContext().getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return "login?faces-redirect=true";
    }
    // ==================== HELPERS ====================
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }
}
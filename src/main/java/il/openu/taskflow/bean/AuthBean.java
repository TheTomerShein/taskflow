package il.openu.taskflow.bean;

import il.openu.taskflow.entity.User;
import il.openu.taskflow.service.UserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.logging.Logger;

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

        System.out.println("dasokdsoakodsok");

        if (user != null) {
            this.currentUser = user;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Login successful", "Welcome " + username));
            return "dashboard?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Invalid username or password"));
            return null;
        }
    }

    // ==================== REGISTER ====================
    public String register() {
        User newUser = userService.register(username, email, password);

        if (newUser != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Registration successful", "You can now login"));
            return "login?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration failed", "Username or email already exists"));
            return null;
        }
    }

    public String logout() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);

            if (session != null) {
                session.invalidate();
            }

            // ניקוי ה-Bean
            this.currentUser = null;
            this.username = null;
            this.password = null;

            return "login?faces-redirect=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "login?faces-redirect=true";
        }
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
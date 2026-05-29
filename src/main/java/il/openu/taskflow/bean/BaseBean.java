package il.openu.taskflow.bean;

import il.openu.taskflow.entity.User;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;

import java.io.Serializable;

/**
 * Abstract base class for all JSF managed beans.
 * Provides common utility methods for FacesMessage handling
 * and authenticated user retrieval.
 */
public abstract class BaseBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuthBean authBean;

    // ==================== FacesMessage Helpers ====================

    /**
     * Adds an INFO-level FacesMessage to the current context.
     */
    protected void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    /**
     * Adds an ERROR-level FacesMessage to the current context.
     */
    protected void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    /**
     * Adds a WARN-level FacesMessage to the current context.
     */
    protected void addWarnMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, summary, detail));
    }

    // ==================== Auth Helpers ====================

    /**
     * Returns the currently authenticated user, or null if not logged in.
     * When the user is not authenticated, an error message is automatically added.
     *
     * @return the current User, or null if unauthenticated
     */
    protected User getAuthenticatedUser() {
        User user = authBean.getCurrentUser();
        if (user == null) {
            addErrorMessage("Error", "User not authenticated");
        }
        return user;
    }

    /**
     * Returns the underlying AuthBean for subclasses that need
     * direct access (e.g., checking user without adding a message).
     */
    protected AuthBean getAuthBean() {
        return authBean;
    }
}

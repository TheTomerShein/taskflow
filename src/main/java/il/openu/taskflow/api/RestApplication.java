package il.openu.taskflow.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Bootstraps the JAX-RS (RESTful Web Services) application.
 * Defines the base URI path for all REST APIs in the system.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}

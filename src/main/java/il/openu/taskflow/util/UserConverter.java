package il.openu.taskflow.util;

import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.UserRepository;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * JSF Converter for User entity.
 * Required by PrimeFaces p:autoComplete to convert between User objects and their IDs.
 */
@Named
@ApplicationScoped
public class UserConverter implements Converter<User> {

    @Inject
    private UserRepository userRepository;

    @Override
    public User getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.valueOf(value);
            return userRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, User user) {
        if (user == null || user.getId() == null) {
            return "";
        }
        return String.valueOf(user.getId());
    }
}

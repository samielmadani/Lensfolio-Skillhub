package nz.ac.canterbury.seng302.portfolio.exceptions;

import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(UserRole requiredRole) {
        super("User must be a " + requiredRole + " to have access.");
    }

    public ForbiddenException() {
        super("User must be a " + UserRole.TEACHER + " to have access.");
    }
}

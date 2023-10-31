package nz.ac.canterbury.seng302.portfolio.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String name, int id) {
        super("Could not find " + name + " with ID: " + id);
    }
}
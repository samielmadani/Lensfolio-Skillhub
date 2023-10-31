package nz.ac.canterbury.seng302.portfolio.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message ) {
        super(message);
    }
}

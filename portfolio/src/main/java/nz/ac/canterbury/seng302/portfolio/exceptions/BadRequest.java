package nz.ac.canterbury.seng302.portfolio.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequest extends RuntimeException {
    public BadRequest() {
        super("Bad Request");
    }

    public BadRequest(String message) {
        super(message);
    }
}

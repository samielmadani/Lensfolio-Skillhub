package nz.ac.canterbury.seng302.portfolio.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerException extends RuntimeException {
    public ServerException(String message) {
        super(message);
    }

    public ServerException() {
        super("Something went wrong.");
    }
}
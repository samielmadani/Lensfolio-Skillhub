package nz.ac.canterbury.seng302.portfolio.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MilestoneNotFoundException extends RuntimeException{
    public MilestoneNotFoundException(int milestoneId) {
        super("Couldn't find milestone with id: " + milestoneId);
    }
}

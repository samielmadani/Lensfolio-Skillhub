package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.service.DeadlineService;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import org.springframework.stereotype.Service;

@Service
public class DeadlineValidator {
    /**
     * Checks that a deadline has the correct dates and name values.
     * @param currentDeadline Deadline to check for errors
     */
    public static void validateDeadline (Deadline currentDeadline) {
        if (currentDeadline.getName().replaceAll("\\s", "").equals("")) {
            DeadlineService.addNewError(new UserError("DeadlineName", "Deadline name cannot be null!"));
        }
    }
}

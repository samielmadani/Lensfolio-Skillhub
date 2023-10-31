package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import org.springframework.stereotype.Service;

@Service
public class EventValidator {
    /**
     * Checks that an event has the correct dates and name values.
     * @param currentEvent Event to check for errors
     */
    public static void validateEvent (Event currentEvent) {
        if (currentEvent.getName().replaceAll("\\s", "").equals("")) {
            EventService.addNewError(new UserError("EventName", "Event name cannot be null!"));
        }

        if (currentEvent.getStartDate().after(currentEvent.getEndDate())) {
            EventService.addNewError(new UserError("EventDates", "Event start date cannot occur before " +
                    "the end date!"));
        }
    }
}
